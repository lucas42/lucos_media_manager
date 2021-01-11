run: build
	java -cp .:bin:libs/* Manager

build: libs/gson-2.8.1.jar
	mkdir -p bin
	javac -cp .:libs/* -d bin *.java -Xlint:unchecked

build-tests: build libs/junit-platform-console-standalone-1.7.0.jar
	javac -cp .:libs/* -d bin tests/*.java -Xlint:unchecked

test: build-tests libs/junit-platform-console-standalone-1.7.0.jar libs/jacoco/lib/jacocoagent.jar
	java -javaagent:libs/jacoco/lib/jacocoagent.jar -jar libs/junit-platform-console-standalone-1.7.0.jar --scan-classpath --class-path bin:libs/gson-2.8.1.jar
	java -jar libs/jacoco/lib/jacococli.jar report jacoco.exec --html ./reports --classfiles bin --sourcefiles .

libs/gson-2.8.1.jar:
	mkdir -p libs
	wget "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.1/gson-2.8.1.jar" -P libs

libs/junit-platform-console-standalone-1.7.0.jar:
	mkdir -p libs
	wget "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.7.0/junit-platform-console-standalone-1.7.0.jar" -P libs

libs/jacoco/lib/jacocoagent.jar:
	mkdir -p libs
	wget "https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.6/jacoco-0.8.6.zip" -P libs
	unzip libs/jacoco-0.8.6.zip -d libs/jacoco