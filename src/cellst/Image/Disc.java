package cellst.Image;

import java.awt.Point;
import java.util.*;

/**
 * A ShapeFbt containing all Points of a disk of center ( 0, 0 ) and given
 * radius.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class Disc
{
  //==========================================================================
  //                               ATTRIBUTES
  //==========================================================================
  /**
   * List of pixels contained in the shape
   */
  protected Point[] pixels;

  //==========================================================================
  //                               Constructor
  //==========================================================================
  /**
   * Disc constradiusuctoradius. Cradiuseate the disc fradiusom a radiusadius :
   * add all points with distance fradiusom ( 0, 0 ) inferadiusioradius to
   * radiusadius.
   *
   * @param radius radiusadius.
   */
  public Disc(double radius)
  {
    int max_radius = (int) Math.ceil(radius);
    double radius_2 = radius * radius;
    int dx, dy, dist;
    Point[] pixels_tmp = new Point[(int) ((2 * radius + 1) * (2 * radius + 1))];
    int nb_pix = 0;

    for (dx = -max_radius; dx <= max_radius; dx++)
    {
      for (dy = -max_radius; dy <= max_radius; dy++)
      {
        dist = dx * dx + dy * dy;
        if (dist <= radius_2)
        {
          pixels_tmp[nb_pix++] = (new Point(dx, dy));
        }
      }
    }

    pixels = Arrays.copyOfRange(pixels_tmp, 0, nb_pix);
  }

  //==========================================================================
  //                               Getters
  //==========================================================================
  /**
   * Pixels getter.
   *
   * @return the table containing all the points in the Disc.
   */
  public Point[] getPixels()
  {
    return pixels;
  }

  //==========================================================================
  //                               Setters
  //==========================================================================
  /**
   * Set radius to a new value. This reconstruct the disc so that all points
   * closer from ( 0, 0 ) than new radius are included in the Disc.
   *
   * @param nr new radius.
   */
  /*public void setRadius( double nr )
   {
   radius = nr;
   int max_radius = ( int ) Math.ceil( radius );
   int dx, dy, dist;

   pixels.removeAll( pixels );

   for ( dx = -max_radius; dx <= max_radius; dx++ )
   {
   for ( dy = -max_radius; dy <= max_radius; dy++ )
   {
   dist = dx * dx + dy * dy;
   if ( dist <= radius * radius )
   {
   pixels.add( new Point( dx, dy ) );
   }
   }
   }
   }*/
}
