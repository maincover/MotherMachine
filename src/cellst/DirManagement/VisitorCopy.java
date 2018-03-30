package cellst.DirManagement;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

/**
 * FileVisitor, copying a whole directory with its subdirectories and files to a
 * new path.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class VisitorCopy implements FileVisitor<Path>
{
  // ==========================================================================
  //                             ATTRIBUTES
  // ==========================================================================
  /**
   * Path of the directory to copy.
   */
  private Path origin;
  /**
   * Path where to copy directory.
   */
  private Path target;

  /**
   * Copying option.
   */
  private StandardCopyOption option;

  // ==========================================================================
  //                             CONSTRUCTOR
  // ==========================================================================
  /**
   * Creates a Visitor copy.
   *
   * @param _origin
   * @param _target
   * @param _option
   */
  public VisitorCopy(Path _origin, Path _target, StandardCopyOption _option)
  {
    super();

    origin = _origin;
    target = _target;
    option = _option;
  }

  /**
   * Creates a VisitorCopy with copying option REPLACE_EXISTING.
   *
   * @param _origin
   * @param _target
   */
  public VisitorCopy(Path _origin, Path _target)
  {
    this(_origin, _target, StandardCopyOption.REPLACE_EXISTING);
  }

  // ==========================================================================
  //                            VISITING METHODS
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
    // ===== Compute new directory path where we want to copy the directory ===
    Path newPath = target.resolve(origin.relativize(currDir));

    // ==== If it doesn't exists create it ====================================
    if (!Files.exists(newPath))
    {
      Files.createDirectory(newPath);
    }

    // Continue
    return FileVisitResult.CONTINUE;
  }

  /**
   * Invoked for a file in a directory.
   *
   * Copy this file.
   *
   * @param currPath reference for the file
   * @param bfa file's basic attributes
   * @return
   * @throws IOException
   */
  @Override
  public FileVisitResult visitFile(Path currPath, BasicFileAttributes bfa)
      throws IOException
  {
    // ==== Compute new File path where we want to copy file ==================
    Path copyPath = target.resolve(origin.relativize(currPath));

    // === If the file is already in the directory and has not changed ========
    //      do noting 
    if (Files.exists(copyPath))
    {
      if (Files.getLastModifiedTime(copyPath).equals(Files.getLastModifiedTime(
          currPath)))
      {
        return FileVisitResult.CONTINUE;
      }
    }

    // ==== Copy file =========================================================
    Files.copy(currPath, copyPath, option);

    // ============= Continue ================================================
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
    // nothing to do.
    return FileVisitResult.CONTINUE;
  }
}
