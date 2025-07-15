package com.example.ciphershield;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanEncryptionUtil {

    public static class Result {
        public byte[] encryptedData;
        public byte[] treeBytes;

        public Result(byte[] encryptedData, byte[] treeBytes) {
            this.encryptedData = encryptedData;
            this.treeBytes = treeBytes;
        }
    }

    private static class Node implements Comparable<Node> {
        Byte data;
        int freq;
        Node left, right;

        Node(Byte data, int freq) {
            this.data = data;
            this.freq = freq;
        }

        @Override
        public int compareTo(Node other) {
            return this.freq - other.freq;
        }
    }

    public static Result compress(byte[] inputBytes, String originalExtension) throws Exception {
        // 1. Build frequency map
        Map<Byte, Integer> freqMap = new HashMap<>();
        for (byte b : inputBytes) {
            freqMap.put(b, freqMap.getOrDefault(b, 0) + 1);
        }

        // 2. Build Huffman tree
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : freqMap.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node(null, left.freq + right.freq);
            parent.left = left;
            parent.right = right;
            pq.add(parent);
        }

        Node root = pq.poll();

        // 3. Build code map
        Map<Byte, String> codeMap = new HashMap<>();
        buildCodeMap(root, "", codeMap);

        // 4. Encode data
        StringBuilder encodedStr = new StringBuilder();
        for (byte b : inputBytes) {
            encodedStr.append(codeMap.get(b));
        }

        // 5. Convert to byte array
        byte[] compressedData = bitStringToByteArray(encodedStr.toString());

        // 6. Serialize tree
        ByteArrayOutputStream treeStream = new ByteArrayOutputStream();
        serializeTree(root, treeStream);
        byte[] treeBytes = treeStream.toByteArray();

        // 7. Add header: [HUF][extLen][ext][compressed data]
        byte[] methodBytes = "HUF".getBytes(StandardCharsets.UTF_8);
        byte[] extBytes = originalExtension.getBytes(StandardCharsets.UTF_8);
        byte[] extLen = ByteBuffer.allocate(4).putInt(extBytes.length).array();

        ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();
        finalOutput.write(methodBytes);
        finalOutput.write(extLen);
        finalOutput.write(extBytes);
        finalOutput.write(compressedData);

        return new Result(finalOutput.toByteArray(), treeBytes);
    }

    private static void buildCodeMap(Node node, String code, Map<Byte, String> codeMap) {
        if (node == null) return;
        if (node.data != null) {
            codeMap.put(node.data, code.isEmpty() ? "0" : code);
            return;
        }
        buildCodeMap(node.left, code + "0", codeMap);
        buildCodeMap(node.right, code + "1", codeMap);
    }

    private static byte[] bitStringToByteArray(String bitString) {
        int len = bitString.length();
        int byteLen = (len + 7) / 8;
        byte[] data = new byte[byteLen];

        for (int i = 0; i < len; i++) {
            if (bitString.charAt(i) == '1') {
                data[i / 8] |= (1 << (7 - (i % 8)));
            }
        }
        return data;
    }

    private static void serializeTree(Node node, ByteArrayOutputStream out) {
        if (node == null) return;
        if (node.data != null) {
            out.write(1);
            out.write(node.data);
        } else {
            out.write(0);
            serializeTree(node.left, out);
            serializeTree(node.right, out);
        }
    }

    public static byte[] decompress(byte[] compressedData, byte[] treeBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(compressedData);
        String method = new String(compressedData, 0, 3, StandardCharsets.UTF_8);
        if (!"HUF".equals(method)) {
            throw new Exception("Invalid Huffman compressed data");
        }

        int extLen = buffer.getInt(3);
        // Skip extension (7 = 3(method) + 4(extLen))
        byte[] data = Arrays.copyOfRange(compressedData, 7 + extLen, compressedData.length);

        // Deserialize tree
        Node root = deserializeTree(new ByteArrayInputStream(treeBytes));

        // Decode data
        StringBuilder bitString = new StringBuilder();
        for (byte b : data) {
            bitString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0'));
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Node current = root;
        for (int i = 0; i < bitString.length(); i++) {
            char bit = bitString.charAt(i);
            current = (bit == '0') ? current.left : current.right;

            if (current.data != null) {
                output.write(current.data);
                current = root;
            }
        }
        return output.toByteArray();
    }

    private static Node deserializeTree(ByteArrayInputStream in) {
        int flag = in.read();
        if (flag == 1) {
            return new Node((byte) in.read(), 0);
        } else {
            Node node = new Node(null, 0);
            node.left = deserializeTree(in);
            node.right = deserializeTree(in);
            return node;
        }
    }
}