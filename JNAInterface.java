import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.unix.LibCAPI;

public class JNAInterface {
  static int O_RDONLY = 00; // Open read-only.
  static int O_WRONLY = 01; // Open write-only.
  static int O_RDWR = 02;   // Open read/write.

  /*
   * Bits OR'd into the second argument to open. Note these are defined
   * differently on linux than unix fcntl header
   */
  static int O_CREAT = Platform.isMac() ? 01000 : 0100;  // Create file if it doesn't exist. 512
  static int O_EXCL = Platform.isMac() ? 04000 : 0200;   // Fail if file already exists.
  static int O_TRUNC = Platform.isMac() ? 02000 : 01000; // Truncate file to zero length.

  static int S_IRUSR = 00400; // Read by owner.
  static int S_IWUSR = 00200; // Write by owner.
  static int S_IXUSR = 00100; // Execute by owner.
  static int S_IRWXU = S_IRUSR | S_IWUSR | S_IXUSR;

  static int S_IRGRP = 00040; // Read by group.
  static int S_IWGRP = 00020; // Write by group.
  static int S_IXGRP = 00010; // Execute by group.
  static int S_IRWXG = S_IRGRP | S_IWGRP | S_IXGRP;

  static int S_IROTH = 00004; // Read by others.
  static int S_IWOTH = 00002; // Write by others.
  static int S_IXOTH = 00001; // Execute by others.
  static int S_IRWXO = S_IROTH | S_IWOTH | S_IXOTH;

  static int S_ISUID = 04000; // set-user-ID bit
  static int S_ISGID = 02000; // set-group-ID bit (see inode(7)).
  static int S_ISVTX = 01000; // sticky bit (see inode(7)).

  public interface LibC extends LibCAPI, Library { LibC INSTANCE = Native.load("c", LibC.class); }
  public interface LibRT extends Library {
    LibRT INSTANCE = Native.load(Platform.isMac() ? "c" : "rt", LibRT.class);
    /**
     * Creates and opens a new, or opens an existing, POSIX shared memory object. A
     * POSIX shared memory object is in effect a handle which can be used by
     * unrelated processes to {@code mmap()} the same region of shared memory.
     *
     * @param name
     *            The shared memory object to be created or opened. For portable
     *            use, a shared memory object should be identified by a name of the
     *            form {@code /somename} that is, a null-terminated string of up to
     *            {@code NAME_MAX} (i.e., 255) characters consisting of an initial
     *            slash, followed by one or more characters, none of which are
     *            slashes.
     * @param oflag
     *            A bit mask created by ORing together exactly one of
     *            {@code O_RDONLY} or {@code O_RDWR} and any of the other flags
     *            {@code O_CREAT}, {@code O_EXCL}, or {@code O_TRUNC}.
     * @param mode
     *            When {@code oflag} includes {@code O_CREAT}, the object's
     *            permission bits are set according to the low-order 9 bits of mode,
     *            except that those bits set in the process file mode creation mask
     *            (see {@code umask(2)}) are cleared for the new object.
     * @return On success, returns a file descriptor (a nonnegative integer). On
     *         failure, returns -1. On failure, {@code errno} is set to indicate the
     *         cause of the error.
     */
    int shm_open(String name, int oflag, int mode);

    /**
     * Removes an object previously created by {@link #shm_open}.
     *
     * @param name
     *            The shared memory object to be unlinked.
     * @return returns 0 on success, or -1 on error. On failure, {@code errno} is
     *         set to indicate the cause of the error.
     */
    int shm_unlink(String name);
  }
}
