# Makefile wrapper to Maven

all: target test

target:
	( mvn package )

test:
	( mvn scala:testCompile && ./src/test/resources/build-test-jar.sh )

clean:
	( mvn clean )

distclean: clean

install:

