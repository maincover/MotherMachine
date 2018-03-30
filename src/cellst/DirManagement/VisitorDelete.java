package cellst.DirManagement;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * VisitorFile deleting all files and subdirectories in a directory.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class VisitorDelete implements FileVisitor<Path>
{

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
    // nothing to do.
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
    // Delete all directories once they are empty.
    Files.delete(currDir);
    return FileVisitResult.CONTINUE;
  }
}
