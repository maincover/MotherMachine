/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cellst.DirManagement;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author mvangkeosay
 */
public class VisitorCompare implements FileVisitor<Path>
{

  // ==========================================================================
  //                             ATTRIBUTES
  // ==========================================================================
  private final Path path1;
  private final Path path2;
  public boolean arePathsDiff = false;

  // ==========================================================================
  //                             CONSTRUCTOR
  // ==========================================================================
  public VisitorCompare(Path _path1, Path _path2)
  {
    super();

    path1 = _path1;
    path2 = _path2;

    if (!Files.exists(path1) || !Files.exists(path2))
    {
      throw new IllegalArgumentException("Thread (" + Thread.currentThread().
          getId()
                                         + ") : Exception in VisitorCompare, one of the path doesn't exists");
    }
  }

  // ==========================================================================
  //                            VISITING METHODS
  // ==========================================================================
  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
      throws IOException
  {
    // nothing to do.
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path currPath, BasicFileAttributes attrs)
      throws
      IOException
  {
    // ==== Compute new File path where we want to copy file ==================
    Path comparePath = path2.resolve(path1.relativize(currPath));

    // === If the file is in both directories and unchanged continue ==========
    // Else flag directories has differents and stop 
    if (Files.exists(comparePath))
    {
      if (Files.getLastModifiedTime(comparePath).equals(Files.
          getLastModifiedTime(currPath)))
      {
        return FileVisitResult.CONTINUE;
      }
      else
      {
        arePathsDiff = true;
        return FileVisitResult.TERMINATE;
      }
    }
    else
    {
      arePathsDiff = true;
      return FileVisitResult.TERMINATE;
    }
  }

  @Override
  public FileVisitResult visitFileFailed(Path currFile, IOException ioe) throws
      IOException
  {
    System.out.println("Visit Failed : " + currFile.toString());
    throw ioe;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws
      IOException
  {
    // nothing to do.
    return FileVisitResult.CONTINUE;
  }
}
