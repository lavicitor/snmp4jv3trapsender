# snmp4jv3trapsender

This project sends SNMPv3 traps using the snmp4j library.

Since there aren't any well documented examples I have decided to create one out of my own experience with implementing this feature.

To test that the traps are really being received a `docker-compose.yml` is included in `/src/test/resources/` which will setup an instance of `snmptrapd` running in an Ubuntu container. Running the app with `mvn spring-boot:run` will send a single trap which can be viewed in the container's stdout.

## Local testing

Navigate to `/src/test/resources/` and run `docker compose up`. You can follow the trap receiving side here.

In the project root run `mvn clean package` followed by `mvn spring-boot:run`. This will run the application and send a trap.