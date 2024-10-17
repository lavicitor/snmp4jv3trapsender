FROM ubuntu:noble

# Install required packages
RUN apt-get update && apt-get install -y snmptrapd

# Copy and replace configuration file
COPY snmptrapd.conf /etc/snmp/snmptrapd.conf

# Start SNMP daemon on container startup
CMD ["/usr/sbin/snmptrapd", "-f", "-Lo"]
