package org.fastfilter.tools;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Scanner;

import org.fastfilter.xorplus.XorPlus8;

public class PasswordLookup {

    public static void main(String... args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java " + PasswordLookup.class.getName() + " <filterFileName>\n"
                    + "Requires a filter file generated by " + BuildFilterFile.class.getName());
            return;
        }
        String filterFileName = args[0];
        Scanner scanner = new Scanner(System.in);
        while (true) {
            Console console = System.console();
            String password;
            if (console != null) {
                password = new String(console.readPassword("Password? "));
            } else {
                System.out.println("Password? ");
                password = scanner.nextLine();
            }
            if (password.length() == 0) {
                break;
            }
            testPassword(filterFileName, password);
        }
        scanner.close();
    }

    private static void testPassword(String filterFileName, String password) throws Exception {
        byte[] passwordBytes = password.getBytes(Charset.forName("ISO-8859-1"));
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(passwordBytes);
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (sha1[i] & 0xff);
        }
        // set the lowest bit to 0
        long key = hash ^ (hash & 1);
        RandomAccessFile f = new RandomAccessFile(filterFileName, "r");
        int segment = (int) (key >>> (64 - BuildFilterFile.SEGMENT_BITS));
        f.seek(segment * 8);
        long skip = f.readLong();
        f.close();
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filterFileName)));
        while (skip > 0) {
            long skipped = in.skip(skip);
            if (skipped <= 0) {
                break;
            }
            skip -= skipped;
        }
        XorPlus8 filter = new XorPlus8(in);
        in.close();
        boolean found = filter.mayContain(key);
        if (found) {
            System.out.println("Found");
        } else {
            found = filter.mayContain(key | 1);
            if (found) {
                System.out.println("Found; common");
            } else {
                System.out.println("Not found");
            }
        }
    }

}
