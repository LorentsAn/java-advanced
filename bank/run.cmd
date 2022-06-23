echo off
cd ..\..\..\..\..\
SET java-solutions=%CD%
SET classpath=info\kgeorgiy\ja\lorents\bank
SET classname=info.kgeorgiy.ja.lorents.bank
SET tmp=%java-solutions%\tmp
mkdir %tmp%
javac -cp %java-solutions%\lib\junit-4.11.jar;%java-solutions%\lib\hamcrest-core-1.3.jar %java-solutions%\%classpath%\*.java -d %tmp%
cd %tmp%
java -cp %java-solutions%\lib\junit-4.11.jar;%java-solutions%\lib\hamcrest-core-1.3.jar;%tmp% org.junit.runner.JUnitCore %classname%.Tests
cd %java-solutions%
rmdir /s /q %tmp%


