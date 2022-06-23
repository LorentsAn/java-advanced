package info.kgeorgiy.ja.lorents.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Walk {

    // :NOTE: SIZE_OF_HASH
    private static final String nameOfAlgorithm = "SHA-1";
    private static final int sizeOfBuffer = 1024;
    private static final int radixOfHash = 16;
    private static final String nullHash = "0".repeat(40);

    private static BigInteger takeHashCode(Path path, MessageDigest md) {
        try (FileInputStream inStream = new FileInputStream(path.toString())) {
            // :NOTE: move to a const value
            // :NOTE: bufferSize
            byte[] data = new byte[sizeOfBuffer];
            int byteRead;
            while ((byteRead = inStream.read(data)) > -1) {
                md.update(data, 0, byteRead);
            }
            byte[] messageDigets = md.digest();
            BigInteger no = new BigInteger(1, messageDigets);
            md.reset();
            return new BigInteger(no.toString(radixOfHash), radixOfHash);
        } catch (IOException e) {
            // :NOTE: new BigInteger
            return new BigInteger(String.valueOf(0), radixOfHash);
        }


    }

    public static void walk(String inputFile, String outputFile) {
        Path inputFilePath;
        Path outputFilePath;
        try {
            inputFilePath = Path.of(inputFile);
            outputFilePath = Path.of(outputFile);
            // :NOTE: input output file
        } catch (InvalidPathException e) {
            System.err.println("Wrong path of file " + e.getMessage());
            return;
        }

        // :NOTE: getParent, redundant NotExists
        Path parentOfOutputFile = outputFilePath.getParent();
        if (parentOfOutputFile != null) {
            try {
                Files.createDirectories(parentOfOutputFile);
            } catch (IOException e) {
                System.err.println("Unable to create directory for file" + e.getMessage());
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
            try {
                MessageDigest md = MessageDigest.getInstance(nameOfAlgorithm);
                Path path;
                String str;
                while ((str = reader.readLine()) != null) {
                    try {
                        path = Path.of(str);
                        writer.write(String.format("%040x %s", takeHashCode(path, md), path.toString()));
                    } catch (InvalidPathException e) {
                        // :NOTE: move to a const value
                        writer.write(String.format("%s %s", nullHash, str));
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("IOException " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("No such Algorithm " + e.getMessage());
            }

        } catch (UnsupportedEncodingException e) {
            System.err.println("Wrong encoding of file " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path to file " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("File not found " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Can't open file " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            // :NOTE: Usage <> <>
            System.err.println("Invalid arguments, expected: <input file> <output file>");
            return;
        }
        walk(args[0], args[1]);
    }
}


