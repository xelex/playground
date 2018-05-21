# Example applications

1. Minimalistic code
1. No spring libs
1. High test coverage - over 80% (according to IntelliJ IDEA)
1. self-runnable jar

## Requirements

1. gradle
1. java 1.8
1. internet connection

## How to build and run

Build jar file via **gradlew** script and and run it: 
1. `./gradlew clean test fatjar`
2. run jar File: `java -jar ./build/libs/playground-1.0-capsule.jar`
