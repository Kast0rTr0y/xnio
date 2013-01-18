/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xnio.ducts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

/**
 * General utility methods for manipulating ducts.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Ducts {

    /**
     * Wrap a duct with zero or more ducts created from factories.
     *
     * @param duct the duct to wrap
     * @param factories the factories to apply
     * @param <D> the duct type
     * @return the wrapped duct
     */
    public static <D extends Duct> D wrap(D duct, DuctFactory<D>... factories) {
        for (DuctFactory<D> factory : factories) {
            duct = factory.create(duct);
        }
        return duct;
    }

    /**
     * Wrap a duct with zero or more ducts created from factories.
     *
     * @param duct the duct to wrap
     * @param factories the factories to apply
     * @param offs the offset into the factories array
     * @param len the number of elements to process in the factories array
     * @param <D> the duct type
     * @return the wrapped duct
     */
    public static <D extends Duct> D wrap(D duct, DuctFactory<D>[] factories, int offs, int len) {
        for (int i = 0; i < len; i++) {
            final DuctFactory<D> factory = factories[i + offs];
            duct = factory.create(duct);
        }
        return duct;
    }

    /**
     * Wrap a duct with zero or more ducts created from factories.
     *
     * @param duct the duct to wrap
     * @param factories the factories to apply
     * @param <D> the duct type
     * @return the wrapped duct
     */
    public static <D extends Duct> D wrap(D duct, Iterable<DuctFactory<D>> factories) {
        for (DuctFactory<D> factory : factories) {
            duct = factory.create(duct);
        }
        return duct;
    }

    /**
     * Wrap a duct with zero or more ducts created from factories.
     *
     * @param duct the duct to wrap
     * @param factories the factories to apply
     * @param <D> the duct type
     * @return the wrapped duct
     */
    public static <D extends Duct> D wrap(D duct, Iterator<DuctFactory<D>> factories) {
        while (factories.hasNext()) {
            duct = factories.next().create(duct);
        }
        return duct;
    }

    /**
     * Platform-independent channel-to-channel transfer method.  Uses regular {@code read} and {@code write} operations
     * to move bytes from the {@code source} channel to the {@code sink} channel.  After this call, the {@code throughBuffer}
     * should be checked for remaining bytes; if there are any, they should be written to the {@code sink} channel before
     * proceeding.  This method may be used with NIO channels, XNIO channels, or a combination of the two.
     * <p>
     * If either or both of the given channels are blocking channels, then this method may block.
     *
     * @param source the source channel to read bytes from
     * @param count the number of bytes to transfer (must be >= {@code 0L})
     * @param throughBuffer the buffer to transfer through (must not be {@code null})
     * @param sink the sink channel to write bytes to
     * @return the number of bytes actually transferred (possibly 0)
     * @throws java.io.IOException if an I/O error occurs during the transfer of bytes
     */
    public static long transfer(final StreamSourceDuct source, final long count, final ByteBuffer throughBuffer, final WritableByteChannel sink) throws IOException {
        long res;
        long total = 0L;
        throughBuffer.clear();
        while (total < count) {
            if (count - total < (long) throughBuffer.remaining()) {
                throughBuffer.limit((int) (count - total));
            }
            try {
                res = source.read(throughBuffer);
                if (res <= 0) {
                    return total == 0L ? res : total;
                }
            } finally {
                throughBuffer.flip();
            }
            res = sink.write(throughBuffer);
            if (res == 0) {
                return total;
            }
            total += res;
            if (total < count) {
                // only compact if nothing is left otherwise we may
                // end up with a buffer that has a lim == cap even
                // if it not contain data that we are interested in
                throughBuffer.compact();
            }
        }
        return total;
    }

    /**
     * Platform-independent channel-to-channel transfer method.  Uses regular {@code read} and {@code write} operations
     * to move bytes from the {@code source} channel to the {@code sink} channel.  After this call, the {@code throughBuffer}
     * should be checked for remaining bytes; if there are any, they should be written to the {@code sink} channel before
     * proceeding.  This method may be used with NIO channels, XNIO channels, or a combination of the two.
     * <p>
     * If either or both of the given channels are blocking channels, then this method may block.
     *
     * @param source the source channel to read bytes from
     * @param count the number of bytes to transfer (must be >= {@code 0L})
     * @param throughBuffer the buffer to transfer through (must not be {@code null})
     * @param sink the sink channel to write bytes to
     * @return the number of bytes actually transferred (possibly 0)
     * @throws java.io.IOException if an I/O error occurs during the transfer of bytes
     */
    public static long transfer(final ReadableByteChannel source, final long count, final ByteBuffer throughBuffer, final StreamSinkDuct sink) throws IOException {
        long res;
        long total = 0L;
        throughBuffer.clear();
        while (total < count) {
            if (count - total < (long) throughBuffer.remaining()) {
                throughBuffer.limit((int) (count - total));
            }
            try {
                res = source.read(throughBuffer);
                if (res <= 0) {
                    return total == 0L ? res : total;
                }
            } finally {
                throughBuffer.flip();
            }
            res = sink.write(throughBuffer);
            if (res == 0) {
                return total;
            }
            total += res;
            if (total < count) {
                // only compact if nothing is left otherwise we may
                // end up with a buffer that has a lim == cap even
                // if it not contain data that we are interested in
                throughBuffer.compact();
            }
        }
        return total;
    }
}