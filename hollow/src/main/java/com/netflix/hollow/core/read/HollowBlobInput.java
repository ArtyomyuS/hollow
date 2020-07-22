package com.netflix.hollow.core.read;

import static com.netflix.hollow.core.memory.MemoryMode.ON_HEAP;
import static com.netflix.hollow.core.memory.MemoryMode.SHARED_MEMORY_LAZY;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.core.memory.MemoryMode;
import com.netflix.hollow.core.memory.encoding.BlobByteBuffer;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * This class provides an abstraction to help navigate between use of DataInputStream or RandomAccessFile
 * as the backing resource for Hollow Producer/Consumer Blob in order to work with the different memory modes.
 */
public class HollowBlobInput implements Closeable {
    private Object input;
    private BlobByteBuffer buffer;

    private HollowBlobInput() {}

    /**
     * Initialize the Hollow Blob Input object from the Hollow Consumer blob's Input Stream or Random Access File, depending on the configured memory mode
     * @param mode Configured memory mode
     * @param blob Hollow Consumer blob
     * @return the initialized Hollow Blob Input
     * @throws IOException if the Hollow Blob Input couldn't be initialized
     */
    public static HollowBlobInput modeBasedSelector(MemoryMode mode, HollowConsumer.Blob blob) throws IOException {
        if (mode.equals(ON_HEAP)) {
            return dataInputStream(blob.getInputStream());
        } else if (mode.equals(SHARED_MEMORY_LAZY)) {
            return randomAccessFile(blob.getFile());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    // SNAP: TODO: Comment about how returned handle must be closed
    public static HollowBlobInput randomAccessFile(File f) throws IOException {
        HollowBlobInput hbi = new HollowBlobInput();
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        hbi.input = raf;
        FileChannel channel = ((RandomAccessFile) hbi.input).getChannel();
        hbi.buffer = BlobByteBuffer.mmapBlob(channel);
        return hbi;
    }

    public static HollowBlobInput dataInputStream(InputStream is) { // SNAP: TODO: Can everything be a RandomAccessFile?
        HollowBlobInput hbi = new HollowBlobInput();
        hbi.input = new DataInputStream(is);
        return hbi;
    }

    /**
     * Reads the next byte of data from the input stream by relaying the call to the underlying {@code DataInputStream} or
     * {@code RandomAccessFile}. The byte is returned as an integer in the range 0 to 255.
     *
     * @return an integer in the range 0 to 255
     * @throws IOException if underlying {@code DataInputStream} or {@code RandomAccessFile}
     * @throws UnsupportedOperationException if the input type wasn't  one of {@code DataInputStream} or {@code RandomAccessFile}
     */
    public int read() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).read();
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).read();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).read(b, off, len);
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).read(b, off, len);
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    /**
     * Sets the file-pointer to the desired offset measured from the beginning of the file by relaying the call to the
     * underlying {@code RandomAccessFile}. Operation not supported if the Hollow Blob Input is an {@code DataInputStream}.
     *
     * @param pos the position in bytes from the beginning of the file at which to set the file pointer to.
     * @exception IOException if originated in the underlying {@code RandomAccessFile} implementation
     * @exception UnsupportedOperationException if called when Hollow Blob Input is not a {@code RandomAccessFile}
     */
    public void seek(long pos) throws IOException {
        if (input instanceof RandomAccessFile) {
            ((RandomAccessFile) input).seek(pos);
        } else if (input instanceof DataInputStream) {
            throw new UnsupportedOperationException("Can not seek on Hollow Blob Input of type DataInputStream");
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public long getFilePointer() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).getFilePointer();
        } else if (input instanceof DataInputStream) {
            throw new UnsupportedOperationException("Can not get file pointer for Hollow Blob Input of type DataInputStream");
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public final short readShort() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).readShort();
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).readShort();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public final int readInt() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).readInt();
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).readInt();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public final long readLong() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).readLong();
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).readLong();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public final String readUTF() throws IOException {
        if (input instanceof RandomAccessFile) {
            return ((RandomAccessFile) input).readUTF();
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).readUTF();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    /**
     * This method attempts to skip a specified number of bytes and returns the actual number of bytes skipped. The
     * behavior is differed based on whether the backing resource is a RandomAccessFile or InputStream. For InputStream,
     * (as implemented in FileInputStream) this method may skip more bytes than what are remaining in the backing file.
     * It will produce no exception and the number of bytes skipped may include some number of bytes that were beyond the
     * EOF of the backing file. The next read attempt from the stream after skipping past the end will result in -1
     * indicating the end of the file was reached. For RandomAccessFile, this method will return the actual bytes skipped
     * and does not go past EOF.
     *
     * @param n number of bytes to skip
     * @return number of bytes skipped
     * @throws IOException
     */
    public long skipBytes(long n) throws IOException {
        if (input instanceof RandomAccessFile) {
            long total = 0;
            int expected = 0;
            int actual = 0;
            do {
                expected = (n-total) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (n-total);
                actual = ((RandomAccessFile) input).skipBytes(expected);    // RandomAccessFile::skipBytes supports int
                total = total + actual;
            } while (total < n && actual > 0);
            return total;
        } else if (input instanceof DataInputStream) {
            return ((DataInputStream) input).skip(n); // InputStream::skip supports long
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    @Override
    public void close() throws IOException {
        if (input instanceof RandomAccessFile) {
            ((RandomAccessFile) input).close();
        } else if (input instanceof DataInputStream) {
            ((DataInputStream) input).close();
        } else {
            throw new UnsupportedOperationException("Unknown Hollow Blob Input type");
        }
    }

    public Object getInput() {
        return input;
    }

    public BlobByteBuffer getBuffer() { // SNAP: TODO: Should I put a type check here? performance concerns?
        return buffer;
    }
}
