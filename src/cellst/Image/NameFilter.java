package cellst.Image;

import java.io.*;

/**
 * Name filter on files. Just keep files with names containing basename.
 *
 * @author Magali Vangkeosay, David Parsons
 */
class NameFilter implements FilenameFilter
{

  /**
   * Contructs a name filter.
   *
   * @param _basename string that must be found in files names.
   */
  public NameFilter(String _basename)
  {
    this.basename = _basename;
  }

  /**
   * Condition of file acceptation. File name must begin with basename and
   * finish with a number.
   *
   * @param dir file directory. Not used.
   * @param name file name.
   * @return boolean true : file accepted.
   */
  @Override
  public boolean accept(File dir, String name)
  {
    if (!name.startsWith(basename))
    {
      return false;
    }

    String nb = name.replace(basename, "");
    try
    {
      Integer.parseInt(nb);
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }
  String basename;
}
