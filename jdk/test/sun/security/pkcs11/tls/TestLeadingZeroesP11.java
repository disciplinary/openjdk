/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8014618
 * @summary Need to strip leading zeros in TlsPremasterSecret of DHKeyAgreement
 * @library ..
 * @author Pasi Eronen
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestLeadingZeroesP11
 * @run main/othervm TestLeadingZeroesP11 sm
 */


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;

/**
 * Test that leading zeroes are stripped in TlsPremasterSecret case,
 * but are left as-is in other cases.
 *
 * We use pre-generated keypairs, since with randomly generated keypairs,
 * a leading zero happens only (roughly) 1 out of 256 cases.
 */

public class TestLeadingZeroesP11 extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestLeadingZeroesP11(), args);
    }

    @Override
    public void main(Provider p) throws Exception {

        // decode pre-generated keypairs
        KeyFactory kfac = KeyFactory.getInstance("DH", p);
        PublicKey alicePubKey =
            kfac.generatePublic(new X509EncodedKeySpec(alicePubKeyEnc));
        PublicKey bobPubKey =
            kfac.generatePublic(new X509EncodedKeySpec(bobPubKeyEnc));
        PrivateKey alicePrivKey =
            kfac.generatePrivate(new PKCS8EncodedKeySpec(alicePrivKeyEnc));
        PrivateKey bobPrivKey =
            kfac.generatePrivate(new PKCS8EncodedKeySpec(bobPrivKeyEnc));

        // generate normal shared secret
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH", p);
        aliceKeyAgree.init(alicePrivKey);
        aliceKeyAgree.doPhase(bobPubKey, true);
        byte[] sharedSecret = aliceKeyAgree.generateSecret();
        System.out.println("shared secret:\n" + toHexString(sharedSecret));

        // verify that leading zero is present
        if (sharedSecret.length != 128) {
            throw new Exception("Unexpected shared secret length");
        }
        if (sharedSecret[0] != 0) {
            throw new Exception("First byte is not zero as expected");
        }

        // now, test TLS premaster secret
        aliceKeyAgree.init(alicePrivKey);
        aliceKeyAgree.doPhase(bobPubKey, true);
        byte[] tlsPremasterSecret =
            aliceKeyAgree.generateSecret("TlsPremasterSecret").getEncoded();
        System.out.println(
            "tls premaster secret:\n" + toHexString(tlsPremasterSecret));

        // check that leading zero has been stripped
        if (tlsPremasterSecret.length != 127) {
            throw new Exception("Unexpected TLS premaster secret length");
        }
        if (tlsPremasterSecret[0] == 0) {
            throw new Exception("First byte is zero");
        }
        for (int i = 0; i < tlsPremasterSecret.length; i++) {
            if (tlsPremasterSecret[i] != sharedSecret[i+1]) {
                throw new Exception("Shared secrets differ");
            }
        }

    }

    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                            '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    private String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();

        int len = block.length;

        for (int i = 0; i < len; i++) {
             byte2hex(block[i], buf);
             if (i < len-1) {
                 buf.append(":");
             }
        }
        return buf.toString();
    }

    private static final byte alicePubKeyEnc[] = {
        (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x24,
        (byte)0x30, (byte)0x81, (byte)0x99, (byte)0x06,
        (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48,
        (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01,
        (byte)0x03, (byte)0x01, (byte)0x30, (byte)0x81,
        (byte)0x8B, (byte)0x02, (byte)0x81, (byte)0x81,
        (byte)0x00, (byte)0xF4, (byte)0x88, (byte)0xFD,
        (byte)0x58, (byte)0x4E, (byte)0x49, (byte)0xDB,
        (byte)0xCD, (byte)0x20, (byte)0xB4, (byte)0x9D,
        (byte)0xE4, (byte)0x91, (byte)0x07, (byte)0x36,
        (byte)0x6B, (byte)0x33, (byte)0x6C, (byte)0x38,
        (byte)0x0D, (byte)0x45, (byte)0x1D, (byte)0x0F,
        (byte)0x7C, (byte)0x88, (byte)0xB3, (byte)0x1C,
        (byte)0x7C, (byte)0x5B, (byte)0x2D, (byte)0x8E,
        (byte)0xF6, (byte)0xF3, (byte)0xC9, (byte)0x23,
        (byte)0xC0, (byte)0x43, (byte)0xF0, (byte)0xA5,
        (byte)0x5B, (byte)0x18, (byte)0x8D, (byte)0x8E,
        (byte)0xBB, (byte)0x55, (byte)0x8C, (byte)0xB8,
        (byte)0x5D, (byte)0x38, (byte)0xD3, (byte)0x34,
        (byte)0xFD, (byte)0x7C, (byte)0x17, (byte)0x57,
        (byte)0x43, (byte)0xA3, (byte)0x1D, (byte)0x18,
        (byte)0x6C, (byte)0xDE, (byte)0x33, (byte)0x21,
        (byte)0x2C, (byte)0xB5, (byte)0x2A, (byte)0xFF,
        (byte)0x3C, (byte)0xE1, (byte)0xB1, (byte)0x29,
        (byte)0x40, (byte)0x18, (byte)0x11, (byte)0x8D,
        (byte)0x7C, (byte)0x84, (byte)0xA7, (byte)0x0A,
        (byte)0x72, (byte)0xD6, (byte)0x86, (byte)0xC4,
        (byte)0x03, (byte)0x19, (byte)0xC8, (byte)0x07,
        (byte)0x29, (byte)0x7A, (byte)0xCA, (byte)0x95,
        (byte)0x0C, (byte)0xD9, (byte)0x96, (byte)0x9F,
        (byte)0xAB, (byte)0xD0, (byte)0x0A, (byte)0x50,
        (byte)0x9B, (byte)0x02, (byte)0x46, (byte)0xD3,
        (byte)0x08, (byte)0x3D, (byte)0x66, (byte)0xA4,
        (byte)0x5D, (byte)0x41, (byte)0x9F, (byte)0x9C,
        (byte)0x7C, (byte)0xBD, (byte)0x89, (byte)0x4B,
        (byte)0x22, (byte)0x19, (byte)0x26, (byte)0xBA,
        (byte)0xAB, (byte)0xA2, (byte)0x5E, (byte)0xC3,
        (byte)0x55, (byte)0xE9, (byte)0x2F, (byte)0x78,
        (byte)0xC7, (byte)0x02, (byte)0x01, (byte)0x02,
        (byte)0x02, (byte)0x02, (byte)0x02, (byte)0x00,
        (byte)0x03, (byte)0x81, (byte)0x85, (byte)0x00,
        (byte)0x02, (byte)0x81, (byte)0x81, (byte)0x00,
        (byte)0xEE, (byte)0xD6, (byte)0xB1, (byte)0xA3,
        (byte)0xB4, (byte)0x78, (byte)0x2B, (byte)0x35,
        (byte)0xEF, (byte)0xCD, (byte)0x17, (byte)0x86,
        (byte)0x63, (byte)0x2B, (byte)0x97, (byte)0x0E,
        (byte)0x7A, (byte)0xD1, (byte)0xFF, (byte)0x7A,
        (byte)0xEB, (byte)0x57, (byte)0x61, (byte)0xA1,
        (byte)0xF7, (byte)0x90, (byte)0x11, (byte)0xA7,
        (byte)0x79, (byte)0x28, (byte)0x69, (byte)0xBA,
        (byte)0xA7, (byte)0xB2, (byte)0x37, (byte)0x17,
        (byte)0xAE, (byte)0x3C, (byte)0x92, (byte)0x89,
        (byte)0x88, (byte)0xE5, (byte)0x7E, (byte)0x8E,
        (byte)0xF0, (byte)0x24, (byte)0xD0, (byte)0xE1,
        (byte)0xC4, (byte)0xB0, (byte)0x26, (byte)0x5A,
        (byte)0x1E, (byte)0xBD, (byte)0xA0, (byte)0xCF,
        (byte)0x3E, (byte)0x97, (byte)0x2A, (byte)0x13,
        (byte)0x92, (byte)0x3B, (byte)0x39, (byte)0xD0,
        (byte)0x1D, (byte)0xA3, (byte)0x6B, (byte)0x3E,
        (byte)0xC2, (byte)0xBB, (byte)0x14, (byte)0xB6,
        (byte)0xE2, (byte)0x4C, (byte)0x0E, (byte)0x5B,
        (byte)0x4B, (byte)0xA4, (byte)0x9D, (byte)0xA6,
        (byte)0x21, (byte)0xB0, (byte)0xF9, (byte)0xDE,
        (byte)0x55, (byte)0xAE, (byte)0x5C, (byte)0x29,
        (byte)0x0E, (byte)0xC1, (byte)0xFC, (byte)0xBA,
        (byte)0x51, (byte)0xD3, (byte)0xB6, (byte)0x6D,
        (byte)0x75, (byte)0x72, (byte)0xDF, (byte)0x43,
        (byte)0xAB, (byte)0x94, (byte)0x21, (byte)0x6E,
        (byte)0x0C, (byte)0xD1, (byte)0x93, (byte)0x54,
        (byte)0x56, (byte)0x7D, (byte)0x4B, (byte)0x90,
        (byte)0xF1, (byte)0x94, (byte)0x45, (byte)0xD4,
        (byte)0x2A, (byte)0x71, (byte)0xA1, (byte)0xB8,
        (byte)0xDD, (byte)0xAA, (byte)0x05, (byte)0xF0,
        (byte)0x27, (byte)0x37, (byte)0xBD, (byte)0x44
    };

    private static final byte alicePrivKeyEnc[] = {
        (byte)0x30, (byte)0x81, (byte)0xE3, (byte)0x02,
        (byte)0x01, (byte)0x00, (byte)0x30, (byte)0x81,
        (byte)0x99, (byte)0x06, (byte)0x09, (byte)0x2A,
        (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7,
        (byte)0x0D, (byte)0x01, (byte)0x03, (byte)0x01,
        (byte)0x30, (byte)0x81, (byte)0x8B, (byte)0x02,
        (byte)0x81, (byte)0x81, (byte)0x00, (byte)0xF4,
        (byte)0x88, (byte)0xFD, (byte)0x58, (byte)0x4E,
        (byte)0x49, (byte)0xDB, (byte)0xCD, (byte)0x20,
        (byte)0xB4, (byte)0x9D, (byte)0xE4, (byte)0x91,
        (byte)0x07, (byte)0x36, (byte)0x6B, (byte)0x33,
        (byte)0x6C, (byte)0x38, (byte)0x0D, (byte)0x45,
        (byte)0x1D, (byte)0x0F, (byte)0x7C, (byte)0x88,
        (byte)0xB3, (byte)0x1C, (byte)0x7C, (byte)0x5B,
        (byte)0x2D, (byte)0x8E, (byte)0xF6, (byte)0xF3,
        (byte)0xC9, (byte)0x23, (byte)0xC0, (byte)0x43,
        (byte)0xF0, (byte)0xA5, (byte)0x5B, (byte)0x18,
        (byte)0x8D, (byte)0x8E, (byte)0xBB, (byte)0x55,
        (byte)0x8C, (byte)0xB8, (byte)0x5D, (byte)0x38,
        (byte)0xD3, (byte)0x34, (byte)0xFD, (byte)0x7C,
        (byte)0x17, (byte)0x57, (byte)0x43, (byte)0xA3,
        (byte)0x1D, (byte)0x18, (byte)0x6C, (byte)0xDE,
        (byte)0x33, (byte)0x21, (byte)0x2C, (byte)0xB5,
        (byte)0x2A, (byte)0xFF, (byte)0x3C, (byte)0xE1,
        (byte)0xB1, (byte)0x29, (byte)0x40, (byte)0x18,
        (byte)0x11, (byte)0x8D, (byte)0x7C, (byte)0x84,
        (byte)0xA7, (byte)0x0A, (byte)0x72, (byte)0xD6,
        (byte)0x86, (byte)0xC4, (byte)0x03, (byte)0x19,
        (byte)0xC8, (byte)0x07, (byte)0x29, (byte)0x7A,
        (byte)0xCA, (byte)0x95, (byte)0x0C, (byte)0xD9,
        (byte)0x96, (byte)0x9F, (byte)0xAB, (byte)0xD0,
        (byte)0x0A, (byte)0x50, (byte)0x9B, (byte)0x02,
        (byte)0x46, (byte)0xD3, (byte)0x08, (byte)0x3D,
        (byte)0x66, (byte)0xA4, (byte)0x5D, (byte)0x41,
        (byte)0x9F, (byte)0x9C, (byte)0x7C, (byte)0xBD,
        (byte)0x89, (byte)0x4B, (byte)0x22, (byte)0x19,
        (byte)0x26, (byte)0xBA, (byte)0xAB, (byte)0xA2,
        (byte)0x5E, (byte)0xC3, (byte)0x55, (byte)0xE9,
        (byte)0x2F, (byte)0x78, (byte)0xC7, (byte)0x02,
        (byte)0x01, (byte)0x02, (byte)0x02, (byte)0x02,
        (byte)0x02, (byte)0x00, (byte)0x04, (byte)0x42,
        (byte)0x02, (byte)0x40, (byte)0x36, (byte)0x4D,
        (byte)0xD0, (byte)0x58, (byte)0x64, (byte)0x91,
        (byte)0x78, (byte)0xA2, (byte)0x4B, (byte)0x79,
        (byte)0x46, (byte)0xFE, (byte)0xC9, (byte)0xD9,
        (byte)0xCA, (byte)0x5C, (byte)0xF9, (byte)0xFD,
        (byte)0x6C, (byte)0x5D, (byte)0x76, (byte)0x3A,
        (byte)0x41, (byte)0x6D, (byte)0x44, (byte)0x62,
        (byte)0x75, (byte)0x93, (byte)0x81, (byte)0x93,
        (byte)0x00, (byte)0x4C, (byte)0xB1, (byte)0xD8,
        (byte)0x7D, (byte)0x9D, (byte)0xF3, (byte)0x16,
        (byte)0x2C, (byte)0x6C, (byte)0x9F, (byte)0x7A,
        (byte)0x84, (byte)0xA3, (byte)0x7A, (byte)0xC1,
        (byte)0x4F, (byte)0x60, (byte)0xE3, (byte)0xB5,
        (byte)0x86, (byte)0x28, (byte)0x08, (byte)0x4D,
        (byte)0x94, (byte)0xB6, (byte)0x04, (byte)0x0D,
        (byte)0xAC, (byte)0xBD, (byte)0x1F, (byte)0x42,
        (byte)0x8F, (byte)0x1B
    };

    private static final byte bobPubKeyEnc[] = {
        (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x23,
        (byte)0x30, (byte)0x81, (byte)0x99, (byte)0x06,
        (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48,
        (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01,
        (byte)0x03, (byte)0x01, (byte)0x30, (byte)0x81,
        (byte)0x8B, (byte)0x02, (byte)0x81, (byte)0x81,
        (byte)0x00, (byte)0xF4, (byte)0x88, (byte)0xFD,
        (byte)0x58, (byte)0x4E, (byte)0x49, (byte)0xDB,
        (byte)0xCD, (byte)0x20, (byte)0xB4, (byte)0x9D,
        (byte)0xE4, (byte)0x91, (byte)0x07, (byte)0x36,
        (byte)0x6B, (byte)0x33, (byte)0x6C, (byte)0x38,
        (byte)0x0D, (byte)0x45, (byte)0x1D, (byte)0x0F,
        (byte)0x7C, (byte)0x88, (byte)0xB3, (byte)0x1C,
        (byte)0x7C, (byte)0x5B, (byte)0x2D, (byte)0x8E,
        (byte)0xF6, (byte)0xF3, (byte)0xC9, (byte)0x23,
        (byte)0xC0, (byte)0x43, (byte)0xF0, (byte)0xA5,
        (byte)0x5B, (byte)0x18, (byte)0x8D, (byte)0x8E,
        (byte)0xBB, (byte)0x55, (byte)0x8C, (byte)0xB8,
        (byte)0x5D, (byte)0x38, (byte)0xD3, (byte)0x34,
        (byte)0xFD, (byte)0x7C, (byte)0x17, (byte)0x57,
        (byte)0x43, (byte)0xA3, (byte)0x1D, (byte)0x18,
        (byte)0x6C, (byte)0xDE, (byte)0x33, (byte)0x21,
        (byte)0x2C, (byte)0xB5, (byte)0x2A, (byte)0xFF,
        (byte)0x3C, (byte)0xE1, (byte)0xB1, (byte)0x29,
        (byte)0x40, (byte)0x18, (byte)0x11, (byte)0x8D,
        (byte)0x7C, (byte)0x84, (byte)0xA7, (byte)0x0A,
        (byte)0x72, (byte)0xD6, (byte)0x86, (byte)0xC4,
        (byte)0x03, (byte)0x19, (byte)0xC8, (byte)0x07,
        (byte)0x29, (byte)0x7A, (byte)0xCA, (byte)0x95,
        (byte)0x0C, (byte)0xD9, (byte)0x96, (byte)0x9F,
        (byte)0xAB, (byte)0xD0, (byte)0x0A, (byte)0x50,
        (byte)0x9B, (byte)0x02, (byte)0x46, (byte)0xD3,
        (byte)0x08, (byte)0x3D, (byte)0x66, (byte)0xA4,
        (byte)0x5D, (byte)0x41, (byte)0x9F, (byte)0x9C,
        (byte)0x7C, (byte)0xBD, (byte)0x89, (byte)0x4B,
        (byte)0x22, (byte)0x19, (byte)0x26, (byte)0xBA,
        (byte)0xAB, (byte)0xA2, (byte)0x5E, (byte)0xC3,
        (byte)0x55, (byte)0xE9, (byte)0x2F, (byte)0x78,
        (byte)0xC7, (byte)0x02, (byte)0x01, (byte)0x02,
        (byte)0x02, (byte)0x02, (byte)0x02, (byte)0x00,
        (byte)0x03, (byte)0x81, (byte)0x84, (byte)0x00,
        (byte)0x02, (byte)0x81, (byte)0x80, (byte)0x2C,
        (byte)0x40, (byte)0xFA, (byte)0xF6, (byte)0xA6,
        (byte)0xF8, (byte)0xAC, (byte)0xC2, (byte)0x4F,
        (byte)0xCD, (byte)0xC7, (byte)0x37, (byte)0x93,
        (byte)0xE5, (byte)0xE4, (byte)0x5E, (byte)0x18,
        (byte)0x14, (byte)0xE6, (byte)0x50, (byte)0xDA,
        (byte)0x55, (byte)0x38, (byte)0x5D, (byte)0x24,
        (byte)0xF5, (byte)0x42, (byte)0x68, (byte)0x5F,
        (byte)0xF5, (byte)0x15, (byte)0xC8, (byte)0x9B,
        (byte)0x5D, (byte)0x06, (byte)0x3D, (byte)0xE1,
        (byte)0x52, (byte)0x2F, (byte)0x98, (byte)0xFF,
        (byte)0x37, (byte)0xBB, (byte)0x75, (byte)0x48,
        (byte)0x48, (byte)0xE9, (byte)0x65, (byte)0x84,
        (byte)0x37, (byte)0xBB, (byte)0xB3, (byte)0xE9,
        (byte)0x36, (byte)0x01, (byte)0xB4, (byte)0x6A,
        (byte)0x1C, (byte)0xB2, (byte)0x11, (byte)0x82,
        (byte)0xCE, (byte)0x3D, (byte)0x65, (byte)0xE5,
        (byte)0x3C, (byte)0x89, (byte)0xE9, (byte)0x52,
        (byte)0x19, (byte)0xBD, (byte)0x58, (byte)0xF6,
        (byte)0xA2, (byte)0x03, (byte)0xA8, (byte)0xB2,
        (byte)0xA5, (byte)0xDB, (byte)0xEB, (byte)0xF5,
        (byte)0x94, (byte)0xF9, (byte)0x46, (byte)0xBE,
        (byte)0x45, (byte)0x4C, (byte)0x65, (byte)0xD2,
        (byte)0xD1, (byte)0xCF, (byte)0xFF, (byte)0xFF,
        (byte)0xFA, (byte)0x38, (byte)0xF1, (byte)0x72,
        (byte)0xAB, (byte)0xB9, (byte)0x14, (byte)0x4E,
        (byte)0xF5, (byte)0xF0, (byte)0x7A, (byte)0x8E,
        (byte)0x45, (byte)0xFD, (byte)0x5B, (byte)0xF9,
        (byte)0xA2, (byte)0x97, (byte)0x1B, (byte)0xAE,
        (byte)0x2C, (byte)0x7B, (byte)0x6B, (byte)0x7C,
        (byte)0x98, (byte)0xFE, (byte)0x58, (byte)0xDD,
        (byte)0xBE, (byte)0xF6, (byte)0x1C, (byte)0x8E,
        (byte)0xD0, (byte)0xA1, (byte)0x72
    };

    private static final byte bobPrivKeyEnc[] = {
        (byte)0x30, (byte)0x81, (byte)0xE4, (byte)0x02,
        (byte)0x01, (byte)0x00, (byte)0x30, (byte)0x81,
        (byte)0x99, (byte)0x06, (byte)0x09, (byte)0x2A,
        (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7,
        (byte)0x0D, (byte)0x01, (byte)0x03, (byte)0x01,
        (byte)0x30, (byte)0x81, (byte)0x8B, (byte)0x02,
        (byte)0x81, (byte)0x81, (byte)0x00, (byte)0xF4,
        (byte)0x88, (byte)0xFD, (byte)0x58, (byte)0x4E,
        (byte)0x49, (byte)0xDB, (byte)0xCD, (byte)0x20,
        (byte)0xB4, (byte)0x9D, (byte)0xE4, (byte)0x91,
        (byte)0x07, (byte)0x36, (byte)0x6B, (byte)0x33,
        (byte)0x6C, (byte)0x38, (byte)0x0D, (byte)0x45,
        (byte)0x1D, (byte)0x0F, (byte)0x7C, (byte)0x88,
        (byte)0xB3, (byte)0x1C, (byte)0x7C, (byte)0x5B,
        (byte)0x2D, (byte)0x8E, (byte)0xF6, (byte)0xF3,
        (byte)0xC9, (byte)0x23, (byte)0xC0, (byte)0x43,
        (byte)0xF0, (byte)0xA5, (byte)0x5B, (byte)0x18,
        (byte)0x8D, (byte)0x8E, (byte)0xBB, (byte)0x55,
        (byte)0x8C, (byte)0xB8, (byte)0x5D, (byte)0x38,
        (byte)0xD3, (byte)0x34, (byte)0xFD, (byte)0x7C,
        (byte)0x17, (byte)0x57, (byte)0x43, (byte)0xA3,
        (byte)0x1D, (byte)0x18, (byte)0x6C, (byte)0xDE,
        (byte)0x33, (byte)0x21, (byte)0x2C, (byte)0xB5,
        (byte)0x2A, (byte)0xFF, (byte)0x3C, (byte)0xE1,
        (byte)0xB1, (byte)0x29, (byte)0x40, (byte)0x18,
        (byte)0x11, (byte)0x8D, (byte)0x7C, (byte)0x84,
        (byte)0xA7, (byte)0x0A, (byte)0x72, (byte)0xD6,
        (byte)0x86, (byte)0xC4, (byte)0x03, (byte)0x19,
        (byte)0xC8, (byte)0x07, (byte)0x29, (byte)0x7A,
        (byte)0xCA, (byte)0x95, (byte)0x0C, (byte)0xD9,
        (byte)0x96, (byte)0x9F, (byte)0xAB, (byte)0xD0,
        (byte)0x0A, (byte)0x50, (byte)0x9B, (byte)0x02,
        (byte)0x46, (byte)0xD3, (byte)0x08, (byte)0x3D,
        (byte)0x66, (byte)0xA4, (byte)0x5D, (byte)0x41,
        (byte)0x9F, (byte)0x9C, (byte)0x7C, (byte)0xBD,
        (byte)0x89, (byte)0x4B, (byte)0x22, (byte)0x19,
        (byte)0x26, (byte)0xBA, (byte)0xAB, (byte)0xA2,
        (byte)0x5E, (byte)0xC3, (byte)0x55, (byte)0xE9,
        (byte)0x2F, (byte)0x78, (byte)0xC7, (byte)0x02,
        (byte)0x01, (byte)0x02, (byte)0x02, (byte)0x02,
        (byte)0x02, (byte)0x00, (byte)0x04, (byte)0x43,
        (byte)0x02, (byte)0x41, (byte)0x00, (byte)0xE0,
        (byte)0x31, (byte)0xE7, (byte)0x77, (byte)0xB8,
        (byte)0xD0, (byte)0x7E, (byte)0x0A, (byte)0x9B,
        (byte)0x94, (byte)0xD5, (byte)0x3D, (byte)0x33,
        (byte)0x62, (byte)0x32, (byte)0x51, (byte)0xCE,
        (byte)0x74, (byte)0x5C, (byte)0xA5, (byte)0x72,
        (byte)0xD9, (byte)0x36, (byte)0xF3, (byte)0x8A,
        (byte)0x3F, (byte)0x8B, (byte)0xC6, (byte)0xFE,
        (byte)0xEF, (byte)0x94, (byte)0x8B, (byte)0x50,
        (byte)0x41, (byte)0x9B, (byte)0x14, (byte)0xC8,
        (byte)0xE9, (byte)0x1F, (byte)0x24, (byte)0x1F,
        (byte)0x65, (byte)0x8E, (byte)0xD3, (byte)0x85,
        (byte)0xD0, (byte)0x68, (byte)0x6C, (byte)0xF1,
        (byte)0x79, (byte)0x45, (byte)0xD0, (byte)0x06,
        (byte)0xA4, (byte)0xB8, (byte)0xE0, (byte)0x64,
        (byte)0xF5, (byte)0x38, (byte)0x72, (byte)0x97,
        (byte)0x00, (byte)0x23, (byte)0x5F
    };
}

