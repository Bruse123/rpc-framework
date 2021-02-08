package com.lhb.rpc.serializer.impl;

import com.lhb.rpc.api.spi.Singleton;
import com.lhb.rpc.serializer.Serializer;

import java.nio.charset.StandardCharsets;

/**
 * String类型的序列化器
 *
 * @author BruseLin
 */
@Singleton
public class StringSerializer implements Serializer<String> {
    @Override
    public byte type() {
        return Type.STRING_TYPE;
    }

    @Override
    public int size(String entry) {
        return entry.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void serialize(String entry, byte[] buffers, int offset, int length) {
        byte[] entryBytes = entry.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(entryBytes, 0, buffers, offset, length);
    }

    @Override
    public String parse(byte[] buffers, int offset, int length) {
        return new String(buffers, offset, length, StandardCharsets.UTF_8);
    }

    @Override
    public Class<String> getSerializeClass() {
        return String.class;
    }
}
