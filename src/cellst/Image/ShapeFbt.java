package cellst.Image;

import java.awt.*;
import java.io.*;
import java.util.*;

import cellst.Main.Utils;

/**
 * A set of points. // TODO : pixels ? + explain a bit more
 *
 * This class can also keep track of its border, it gravity center and radius.
 *
 * @author Magali Vangkeosay, David Parsons
 */
public class ShapeFbt implements Serializable
{
  //=========================================================================
  //                               ATTRIBUTES
  //=========================================================================
  /**
   * List of pixels contained in the shape
   */
  protected HashSet<Point> pixels = new HashSet<Point>();
  /**
   * List of pixels of the border of the shape
   */
  protected HashSet<Point> boundary = new HashSet<Point>();
  /**
   * gravity center
   */
  protected Point center;
  /**
   * Maximum radius from center
   */
  protected double radius;

  //==========================================================================
  //                               Constructor
  //========================================================================== 
  /**
   * ShapeFbt constructor. Create an empty ShapeFbt.
   *
   */
  public ShapeFbt()
  {
  }

  /**
   * ShapeFbt constructor. Create a ShapeFbt and add it all the points in p.
   *
   * @param p List of points to add to the ShapeFbt.
   */
  public ShapeFbt(HashSet<Point> p)
  {
    pixels = p;
  }

  //==========================================================================
  //                               Getters
  //==========================================================================
  /**
   * Pixels getter.
   *
   * @return an HashSet of all points contained in the ShapeFbt.
   */
  public HashSet<Point> getPixels()
  {
    return pixels;
  }

  /**
   * Size getter.
   *
   * @return number of points contained in the ShapeFbt.
   */
  public int getSize()
  {
    return pixels.size();
  }

  /**
   * Radius getter. This may be null if center or radius were not computed
   * before calling the method.
   *
   * @return radius : max distance between a point of the ShapeFbt and its
   * center.
   */
  public double getRadius()
  {
    return radius;
  }

  /**
   * Returns boundary attribute.
   *
   * @return
   */
  public HashSet<Point> getBoundary()
  {
    return boundary;
  }

  /**
   * Returns blob center point.
   *
   * @return blob center point.
   */
  public Point getCenter()
  {
    return center;
  }

  //==========================================================================
  //                               Setters
  //==========================================================================
  /**
   * Pixels setters. Replace all points in shape by the given points HashSet.
   * WARNING : this doesn't update boundaries, centers and radii.
   *
   * @param _pixels new list of points.
   */
  public void setPixels(HashSet<Point> _pixels)
  {
    pixels = _pixels;
  }

  /**
   * Compute center point as center of gravity of the ShapeFbt.
   */
  protected void setCenter()
  {
    Iterator pix_itr = pixels.iterator();
    int sumX = 0;
    int sumY = 0;

    while (pix_itr.hasNext())
    {
      Point pt = (Point) pix_itr.next();
      sumX += pt.x;
      sumY += pt.y;
    }

    sumX /= pixels.size();
    sumY /= pixels.size();

    center = new Point(sumX, sumY);
  }

  /**
   * Compute radius as maximum distance between a point of the ShapeFbt and its
   * center. If center is not compute before this method is called then it is
   * compute in the method.
   */
  protected void setRadius()
  {
    if (center == null)
    {
      setCenter();
    }

    Iterator itr = pixels.iterator();
    radius = 0.;

    while (itr.hasNext())
    {
      Point pt = (Point) itr.next();
      double dist = pt.distance(center);
      if (dist > radius)
      {
        radius = dist;
      }
    }
  }

  /**
   * Compute both center and radius. Use to be sure that radius is compute after
   * center.
   */
  public void setCenterAndRadius()
  {
    setCenter();
    setRadius();
  }

  //==========================================================================
  //                              Public methods
  //==========================================================================
  /**
   * Add a point to the ShapeFbt.
   *
   * @param pt point to add.
   */
  public void add(Point pt)
  {
    pixels.add(pt);
  }

  /**
   * Remove a point from the ShapeFbt.
   *
   * @param pt point to remove.
   */
  public void remove(Point pt)
  {
    pixels.remove(pt);
  }

  /**
   * Get an iterator over all points in the ShapeFbt.
   *
   * @return iterator over all points in the ShapeFbt.
   */
  public Iterator iterator()
  {
    return pixels.iterator();
  }

  /**
   * Compute the ShapeFbt boundary. The boundary is the HashSet of points in the
   * ShapeFbt with at least one neighbor outside the ShapeFbt.
   *
   */
  public void updateBoundary()
  {
    HashSet<Point> bound = new HashSet<Point>();

    // ======== For each pixel of the shape ==================================
    //     1 ) Compute its neighbors pixels 
    //     2 ) If one of its neighbors is outside the shape, add pixel to boundary.
    for (Point currPixel : pixels)
    {
      HashSet<Point> neighs = Utils.getNeigh(currPixel.x, currPixel.y, true);

      for (Point neighPt : neighs)
      {
        if (!pixels.contains(neighPt))
        {
          bound.add(currPixel);
          break;
        }
      }

    }
    boundary = bound;
  }

  /**
   * Compute length of border linking two ShapeFbts. Border's length is the
   * number of points in one of the ShapeFbt having a neighbor in the other
   * ShapeFbt boundary. If this number is not the same for the two ShapeFbt,
   * minimum is taken.
   *
   * @param other other ShapeFbt with which we want to compute border.
   * @return border length between the two ShapeFbts.
   */
  public int border(ShapeFbt other)
  {

    if (center == null)
    {
      setCenterAndRadius();
    }

    if (other.center == null)
    {
      other.setCenterAndRadius();
    }

    // ==========  If too far from each other return 0. ==========
    if (center.distance(other.center) > radius + other.radius)
    {
      return 0;
    }

    // ========== Else compare boundaries point by point ==========
    // border points
    HashSet<Point> border1 = new HashSet<Point>();
    HashSet<Point> border2 = new HashSet<Point>();

    for (Point coord1 : boundary)
    {
      HashSet<Point> neighs = Utils.getNeigh(coord1.x, coord1.y, true);

      for (Point coord2 : neighs)
      {
        // if shapes have neighbors point boundary, increase border
        if (other.boundary.contains(coord2))
        {
          border1.add(coord1);
          border2.add(coord2);
        }

      }
    }

    return Math.min(border1.size(), border2.size());
  }

  /**
   * Approximate the main orientation of the elongation of the union of this
   * ShapeFbt and another one, and tell if the orientation is significant or
   * not. This is used to determined if two blobs can be merge in the same cell.
   *
   * @param other Shape set to merge with.
   * @return Value indication possible cell orientation.
   */
  public double get_orientation_union(ShapeFbt other)
  {
    double size = (double) (getSize() + other.getSize());
    
    if( center == null )
    {
      setCenterAndRadius();
    }
    
    if( other.center == null )
    {
      other.setCenterAndRadius();
    }
    
    double cx = (center.x * (double) getSize() + other.center.x
                                                 * (double) other.getSize())
                / size;
    double cy = (center.y * (double) getSize() + other.center.y
                                                 * (double) other.getSize())
                / size;

    // matrix is | a b |
    //           | b c |
    double a = 0, b = 0, c = 0;

    /* For each pixels of each blob. */
    Iterator itr = iterator();
    while (itr.hasNext())
    {
      Point currPt = (Point) itr.next();
      int x = (int) (currPt.x - cx);
      int y = (int) (currPt.y - cy);
      a += x * x;
      b += x * y;
      c += y * y;
    }

    itr = other.iterator();
    while (itr.hasNext())
    {
      Point currPt = (Point) itr.next();
      int x = (int) (currPt.x - cx);
      int y = (int) (currPt.y - cy);
      a += x * x;
      b += x * y;
      c += y * y;
    }

    a /= size;
    b /= size;
    c /= size;

    /* eigenvalues equation is lam^2 - (a+c)*lam + ac - b^2 = 0 */
    double B = -a - c, C = a * c - b * b;
    double sq_delta = Math.sqrt(B * B - 4 * 1 * C);
    double pe1 = (-B - sq_delta) * 0.5;
    double pe2 = (-B + sq_delta) * 0.5;
    double e1;

    if (Math.abs(pe1) > Math.abs(pe2))
    {
      e1 = pe1;
    }
    else
    {
      e1 = pe2;
    }

    /* first eigenvector */
    double ux, uy;

    if (Math.abs(b) > 0.01)
    {
      ux = 1;
      uy = (e1 - a) / b;
    }
    else if (Math.abs(a - e1) < 0.01)
    {
      ux = 1;
      uy = 0;
    }
    else
    {
      ux = 0;
      uy = 1;
    }
    double theta = Math.atan2(uy, ux);
    return theta;
  }

  /**
   * Approximate the width of the union of the blobs by computing an
   * approximation of the minimum-width annulus containing both blobs.
   *
   * Computing the width of the minimal-area enclosing annulus is equivalent to
   * solving the problem : Data : (x1,y1) ... (xn,yn) Variables : x,y,r^2,R^2
   * Problem : minimize R - r under the constraints For all i = 1..n, r^2
   * inferior or equal to (xi - x)^2 + (yi-y)^2 inferior or equal to R^2
   *
   * This problem is complex to solve in all it's generality, therefore we do an
   * exhaustive search on a limited number of points, we search the center of
   * the annulus in a cone orthogonal to the orientation of the blob. We
   * discretize the cone by taking only a small number of lines in the cone, and
   * only a small number of points on those lines. This seems to give a
   * relatively good approximation.
   *
   * We also only search the minimal-width annulus that encloses the border
   * pixels of the shape (to lighten the computation time), and therefore we
   * only use annulus centers that are outside the shape.
   *
   * @param other over shapeFbt to compare with.
   * @return
   */
  public double getUnionWidth(ShapeFbt other)
  {

    double theta = get_orientation_union(other);
    double size = (double) (getSize() + other.getSize());
    double c_x = (center.x * (double) getSize() + other.center.x
                                                  * (double) other.getSize())
                 / size;
    double c_y = (center.y * (double) getSize() + other.center.y
                                                  * (double) other.getSize())
                 / size;

    double r = 0, R = 1E15;
    double cur_x, cur_y, cur_r, cur_R;

    for (double dt = -Math.PI / 8; dt <= Math.PI / 8; dt += Math.PI / 32)
    {
      double ux = Math.cos(theta + dt + Math.PI / 2.0);
      double uy = Math.sin(theta + dt + Math.PI / 2.0);

      for (double t = -100; t <= 100; t += 10)
      {
        /* XXX
         * We check that |t| >= 20 to avoid centers of annulus inside
         * the shape. This is not a really good method to avoid the
         * center being inside the shape. If it's problematic, and
         * that we can afford the cost, we can check the annulus
         * against all points in the shape rather than only on border
         * pixels, this wouldn't require the center to be outside of
         * the shape */
        if (Math.abs(t) < 20)
        {
          continue;
        }
        cur_x = t * ux + c_x;
        cur_y = t * uy + c_y;

        cur_r = 1E15;
        cur_R = 0;

        for (Point currPt : boundary)
        {
          double px = currPt.x - cur_x;
          double py = currPt.y - cur_y;
          double d = Math.sqrt(px * px + py * py);
          cur_r = Math.min(cur_r, d);
          cur_R = Math.max(cur_R, d);
        }

        for (Point currPt : other.boundary)
        {
          double px = currPt.x - cur_x;
          double py = currPt.y - cur_y;
          double d = Math.sqrt(px * px + py * py);
          cur_r = Math.min(cur_r, d);
          cur_R = Math.max(cur_R, d);
        }

        if (cur_R - cur_r < R - r)
        {
          R = cur_R;
          r = cur_r;
        }
      }
    }

    double width = R - r;

    return width;
  }

}
