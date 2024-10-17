package com.example.snmp4jv3trapsender;

import java.io.IOException;

import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.fluent.SnmpBuilder;
import org.snmp4j.fluent.TargetBuilder;
import org.snmp4j.fluent.TargetBuilder.AuthProtocol;
import org.snmp4j.fluent.TargetBuilder.PrivProtocol;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityProtocols.SecurityProtocolSet;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class TrapSender {

    @Value("${trapsender.destination.ip}")
    private String destinationIp;

    @Value("${trapsender.destination.port}")
    private Integer destinationPort;

    @Value("${trapsender.enterprise}")
    private Integer enterpriseID;

    @Value("${trapsender.username}")
    private String username;

    @Value("${trapsender.authprotocol}")
    private String authProtocol;

    @Value("${trapsender.authpassword}")
    private String authPassword;

    @Value("${trapsender.privprotocol}")
    private String privProtocol;

    @Value("${trapsender.privpassword}")
    private String privPassword;

    @Value("${trapsender.engineid}")
    private String engineIdString;

    @Value("${trapsender.source.ip}")
    private String sourceIp;

    @Autowired
    private EngineIdConfiguration engineIdConfiguration;

    private long systemStartTime;

    private OctetString engineID;

    private Integer localEngineBoot;

    private SnmpBuilder snmpBuilder;

    private Snmp snmp;

    private Target<?> snmpTarget;

    @PostConstruct
    public void ConfigureSnmp() throws IOException, NumberFormatException {

        systemStartTime = System.currentTimeMillis();

        /*
         * Set the unique enterprise ID
         */
        if (this.enterpriseID != null) {
            SNMP4JSettings.setEnterpriseID(enterpriseID);
        }

        /*
         * Using the same engine ID over restarts, if one is not provided a new one will
         * be generated and stored in a file for next time. This is the engine ID which
         * should be used when configuring a user on the trap receiving side.
         */
        if (this.engineIdString == null || this.engineIdString.isEmpty()) {
            String previousEngineId = engineIdConfiguration.getProperty("engine.id");
            if (previousEngineId == null || previousEngineId.isEmpty()) {
                this.engineID = new OctetString(MPv3.createLocalEngineID());
                this.engineIdString = engineID.toString().replaceAll(":", "");
                this.localEngineBoot = 1;
            } else {
                this.engineIdString = previousEngineId;
            }
        }
        this.engineID = OctetString.fromHexStringPairs(this.engineIdString);
        engineIdConfiguration.setProperty("engine.id", this.engineIdString);

        /*
         * When using the same engine ID it is required to keep track of engine
         * restarts, this is a feature of SNMPv3. If a receiver gets a wrong engine boot
         * count it will discard the message.
         */
        if (this.localEngineBoot == null) {
            this.localEngineBoot = Integer.parseInt((engineIdConfiguration.getProperty("engine.boot") != null
                    && !engineIdConfiguration.getProperty("engine.boot").isEmpty())
                            ? engineIdConfiguration.getProperty("engine.boot")
                            : "1");
        }

        this.snmpBuilder = new SnmpBuilder();
        snmpBuilder.securityProtocols(SecurityProtocolSet.maxCompatibility);
        snmp = snmpBuilder.udp().v3().usm().build();
        snmp.setLocalEngine(this.engineID.getValue(), this.localEngineBoot++, 1);
        engineIdConfiguration.setProperty("engine.boot", Integer.toString(this.localEngineBoot));

        this.snmpTarget = createUserTarget(this.destinationIp, this.destinationPort, this.username, this.authProtocol,
                this.privProtocol, this.engineID, this.authPassword, this.privPassword);

        /*
         * A single trap will be sent after application startup.
         * In a real world scenario this line should be removed and the sendTrap method
         * should be used from elsewhere in the code to send traps.
         */
        sendTrap("1.2.3.4.5.6.7.8.9", new VariableBinding[] {
                new VariableBinding(new OID("1.2.3.4.5.6.7.8.9.1"), new OctetString("Trap sent successfully!")) });
    }

    private Target<?> createUserTarget(String address, int port, String username, String auth, String priv,
            OctetString engineId, String authPassphrase, String privPassphrase) {
        if (address == null || username == null) {
            return null;
        }

        TargetBuilder<?> targetBuilder = snmpBuilder.target(UdpAddress.parse(address + "/" + port));
        Target<?> target = targetBuilder
                .user(username, engineId.toByteArray())
                .auth(convertAuthProtocol(auth))
                .authPassphrase(authPassphrase)
                .priv(convertPrivProtocol(priv))
                .privPassphrase(privPassphrase).done().timeout(500).retries(1).build();

        if (privPassphrase != null) {
            target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        } else if (authPassphrase != null) {
            target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
        } else {
            target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
        }

        return target;
    }

    /**
     * Helper method to get snmp4j authentication protocol from a string value
     * 
     * @param authProtocol Authentication protocol in string format
     * @return snmp4j AuthProtocol
     */
    private static AuthProtocol convertAuthProtocol(String authProtocol) {
        switch (authProtocol) {
            case null:
            case "NONE":
                return null;
            case "MD5":
                return AuthProtocol.md5;
            case "SHA":
                return AuthProtocol.sha1;
            default:
                throw new IllegalArgumentException("Unsupported authentication protocol: " + authProtocol);
        }
    }

    /**
     * Helper method to get snmp4j privacy protocol from a string value
     * 
     * @param privProtocol Privacy protocol in string format
     * @return snmp4j PrivProtocol
     */
    private static PrivProtocol convertPrivProtocol(String privProtocol) {
        switch (privProtocol) {
            case null:
            case "NONE":
                return null;
            case "DES":
                return PrivProtocol.des;
            case "AES":
                return PrivProtocol.aes128;
            default:
                throw new IllegalArgumentException("Unsupported privacy protocol: " + privProtocol);
        }
    }

    public boolean sendTrap(String oid, VariableBinding[] vbs) {
        if (this.snmp == null || this.snmpTarget == null) {
            return false;
        }

        try {
            ScopedPDU pdu = new ScopedPDU();
            pdu.setType(PDU.TRAP);

            /*
             * Depending on the kind of SNMP message being sent either the sender or
             * receiver engine ID is considered authoritative. For traps this is the
             * sender's engine ID.
             */
            pdu.setContextEngineID(new OctetString(this.snmp.getLocalEngineID()));

            for (VariableBinding vb : vbs) {
                pdu.add(vb);
            }

            OID trapOid = new OID(oid);
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, trapOid));

            pdu.add(new VariableBinding(SnmpConstants.sysUpTime,
                    new TimeTicks((System.currentTimeMillis() - this.systemStartTime) / 10)));

            if (this.sourceIp != null && !this.sourceIp.isEmpty()) {
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(sourceIp)));
            }

            ResponseEvent<?> event = snmp.send(pdu, this.snmpTarget);
            return event != null && event.getResponse() != null ? true : false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
