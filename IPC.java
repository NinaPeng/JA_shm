import static com.sun.jna.platform.linux.Mman.MAP_SHARED;
import static com.sun.jna.platform.linux.Mman.PROT_READ;
import static com.sun.jna.platform.linux.Mman.PROT_WRITE;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.unix.LibCUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;

public class IPC {
  public static ArrowStreamReader reader = null;
  public static RootAllocator allocator = null;

  public static String randomString(int length) {
    String alphabetsInLowerCase = "abcdefghijklmnopqrstuvwxyz";
    String numbers = "0123456789";
    // create a super set of all characters
    String allCharacters = alphabetsInLowerCase + numbers;
    // initialize a string to hold result
    StringBuilder randomString = new StringBuilder();
    // loop for 10 times
    for (int i = 0; i < length; i++) {
      // generate a random number between 0 and length of all characters
      int randomIndex = (int)(Math.random() * allCharacters.length());
      // retrieve character at index and add it to result
      randomString.append(allCharacters.charAt(randomIndex));
    }
    return randomString.toString();
  }

  public static JSONObject writeSharedRecordBatch(VectorSchemaRoot root) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ArrowStreamWriter writer =
        new ArrowStreamWriter(root, /*DictionaryProvider=*/null, Channels.newChannel(out));
    try {
      writer.start();
      writer.writeBatch();
      writer.end();
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    byte[] data = out.toByteArray();
    String shmName = "/jsm_" + randomString(8);

    int fd = JNAInterface.LibRT.INSTANCE.shm_open(
        shmName, JNAInterface.O_RDWR | JNAInterface.O_CREAT, JNAInterface.S_IRWXU);
    if (fd == -1) {
      System.err.println("can not open shared memory");
      return null;
    }
    // Multiply by 4 to handle all possible encodings
    int bufLen = 4 * data.length;
    size_t length = new size_t(bufLen);

    try {
      int ret = LibCUtil.ftruncate(fd, bufLen);
      if (ret == -1) {
        throw new RuntimeException("Cannot open shared memory");
      }
      Pointer q = LibCUtil.mmap(null, bufLen, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
      ByteBuffer buf = q.getByteBuffer(0, bufLen);
      buf.put(data);
      ret = JNAInterface.LibC.INSTANCE.munmap(q, length);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Cannot shared record batch", e);
    } finally {
      int ret = JNAInterface.LibC.INSTANCE.close(fd);
      try {
        out.close();
      } catch (Exception e) {
        throw new RuntimeException("Cannot close shared memory");
      }
    }

    JSONObject shmObject = new JSONObject();
    shmObject.put("shm_name", shmName.substring(1));
    shmObject.put("shm_size", bufLen);
    return shmObject;
  }

  public static VectorSchemaRoot readSharedRecordBatch(String shmName, long shmSize) {
    allocator = new RootAllocator(Long.MAX_VALUE);
    shmName = "/" + shmName;
    VectorSchemaRoot root;
    int fd =
        JNAInterface.LibRT.INSTANCE.shm_open(shmName, JNAInterface.O_RDWR, JNAInterface.S_IRWXU);
    if (fd == -1) {
      System.err.println("Cannot open shared memory");
      allocator.close();
      return null;
    }
    try {
      Pointer q = LibCUtil.mmap(null, shmSize, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
      byte[] data = q.getByteArray(0, (int)shmSize);
      ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
      reader = new ArrowStreamReader(dataStream, allocator);
      reader.loadNextBatch();
      root = reader.getVectorSchemaRoot();
      int ret = JNAInterface.LibC.INSTANCE.munmap(q, new size_t(shmSize));
    } catch (final Exception e) {
      e.printStackTrace();
      int ret = JNAInterface.LibC.INSTANCE.close(fd);
      throw new RuntimeException("Cannot read shared record batch", e);
    } finally {
      int ret = JNAInterface.LibC.INSTANCE.close(fd);
    }
    return root;
  }
  public static void close() {
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
