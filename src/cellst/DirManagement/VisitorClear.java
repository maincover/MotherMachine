package cellst.DirManagement;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * VisitorFile deleting all files and subdirectories in a directory.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class VisitorClear implements FileVisitor<Path>
{
  // ==========================================================================
  //                ATTRIBUTES
  // ==========================================================================
  /**
   * Starting path that will be cleaned but not deleted.
   */
  private Path startPath;

  /**
   * Boolean to check if it is the first time method preVisitDirectory is
   * called.
   */
  private boolean isStart = true;

  // ==========================================================================
  //                VISITING METHODS
  // ==========================================================================
  /**
   * Invoked for a directory before entries in the directory are visited.
   *
   * If this method returns CONTINUE, then entries in the directory are visited.
   * If this method returns SKIP_SUBTREE or SKIP_SIBLINGS then entries in the
   * directory (and any descendants) will not be visited.
   *
   *
   * @param currDir a reference to the directory
   * @param bfa the directory's basic attributes
   * @return
   * @throws IOException
   */
  @Override
  public FileVisitResult preVisitDirectory(Path currDir, BasicFileAttributes bfa)
      throws IOException
  {
    // =============== If method is called for the first time =================
    // register current directory so it is not deleted later in 
    // postVisitDirectory method.
    if (isStart)
    {
      startPath = currDir;
      isStart = false;
    }

    return FileVisitResult.CONTINUE;
  }

  /**
   * Invoked for a file in a directory.
   *
   * Delete this file.
   *
   * @param currFile reference for the file
   * @param bfa file's basic attributes
   * @return
   * @throws IOException
   */
  @Override
  public FileVisitResult visitFile(Path currFile, BasicFileAttributes bfa)
      throws IOException
  {
    // Delete file and continue.
    Files.delete(currFile);
    return FileVisitResult.CONTINUE;
  }

  /**
   * Invoked for a file that could not be visited.
   *
   * This method is invoked if the file's attributes could not be read, the file
   * is a directory that could not be opened, and other reasons.
   *
   * @param currFile a reference to the file
   * @param ioe the I/O exception that prevented the file from being visited
   * @return
   * @throws IOException
   */
  @Override
  public FileVisitResult visitFileFailed(Path currFile, IOException ioe) throws
      IOException
  {
    System.out.println("Visit Failed : " + currFile.toString());
    throw ioe;
  }

  /**
   * Invoked for a directory after entries in the directory, and all of their
   * descendants, have been visited.
   *
   * This method is also invoked when iteration of the directory completes
   * prematurely (by a visitFile method returning SKIP_SIBLINGS, or an I/O error
   * when iterating over the directory).
   *
   * @param currDir a reference to the directory
   * @param ioe null if the iteration of the directory completes without an
   * error; otherwise the I/O exception that caused the iteration of the
   * directory to complete prematurely
   * @return
   * @throws IOException
   */
  @Override
  public FileVisitResult postVisitDirectory(Path currDir, IOException ioe)
      throws IOException
  {
    // Delete all directories except the starting one.
    if (!currDir.equals(startPath))
    {
      Files.delete(currDir);
    }

    return FileVisitResult.CONTINUE;
  }
}
