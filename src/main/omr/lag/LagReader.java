//----------------------------------------------------------------------------//
//                                                                            //
//                             L a g R e a d e r                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.sheet.Picture;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.util.List;

/**
 * Class <code>LagReader</code> retrieves the runs structure out of a given
 * source. Input is thus the source, the runs are the output, and the provided
 * lag reference is used to access the source pixels using the proper
 * orientation horizontal / vertical.
 *
 * <p>These runs can then be used to populate a lag via {@link SectionsBuilder}.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LagReader
    implements RunsBuilder.Reader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LagReader.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * The lag to populate
     */
    protected final Lag lag;

    /**
     * Runs found in each horizontal row or vertical column
     */
    protected final List<List<Run>> runs;

    /**
     * The source to read runs of pixels from
     */
    protected final PixelSource source;

    /**
     * The maximum pixel grey level to be foreground
     */
    protected final int maxLevel;

    /**
     * The minimum value for a run length to be considered
     */
    protected final int minLength;

    // To avoid too many allocations (beware : non-reentrant)
    private final Point cp = new Point();
    private final Point xy = new Point();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LagReader //
    //-----------//
    /**
     * Create an adapter, with its key parameters.
     *
     * @param lag the lag for which runs have to be filled
     * @param runs the collections of runs to be filled
     * @param source the source to read runs from. Lag orientation is used to
     *               properly access the source pixels.
     * @param minLength the minimum length for each run
     */
    public LagReader (Lag             lag,
                      List<List<Run>> runs,
                      PixelSource     source,
                      int             minLength)
    {
        this.lag = lag;
        this.runs = runs;
        this.source = source;
        this.minLength = minLength;
        this.maxLevel = source.getMaxForeground();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // isFore //
    //--------//
    /**
     * Check whether the provide pixel value is foreground or background
     *
     * @param level pixel grey level
     *
     * @return true if foreground, false if background
     */
    @Implement(RunsBuilder.Reader.class)
    public boolean isFore (int level)
    {
        return level <= maxLevel;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Retrieve the pixel grey level of a point in the underlying source
     *
     * @param coord coordinate value, relative to lag orientation
     * @param pos position value, relative to lag orientation
     *
     * @return pixel grey level
     */
    @Implement(RunsBuilder.Reader.class)
    public int getLevel (int coord,
                         int pos)
    {
        cp.x = coord;
        cp.y = pos;
        lag.switchRef(cp, xy);

        return source.getPixel(xy.x, xy.y);
    }

    //---------//
    // backRun //
    //---------//
    /**
     * Call-back called when a background run has been built
     *
     * @param coord coordinate of run start
     * @param pos position of run start
     * @param length run length
     */
    @Implement(RunsBuilder.Reader.class)
    public void backRun (int coord,
                         int pos,
                         int length)
    {
    }

    //---------//
    // foreRun //
    //---------//
    /**
     * Call-back called when a foreground run has been built
     *
     * @param coord coordinate of run start
     * @param pos position of run start
     * @param length run length
     * @param cumul cumulated pixel grey levels on all run points
     */
    @Implement(RunsBuilder.Reader.class)
    public void foreRun (int coord,
                         int pos,
                         int length,
                         int cumul)
    {
        // We consider only runs that are longer than minLength
        if (length >= minLength) {
            final int level = ((2 * cumul) + length) / (2 * length);
            runs.get(pos)
                .add(new Run(coord - length, length, level));
        }
    }

    //-----------//
    // terminate //
    //-----------//
    /**
     * Method called-back when all runs have been read
     */
    @Implement(RunsBuilder.Reader.class)
    public void terminate ()
    {
        if (logger.isFineEnabled()) {
            StringBuffer buf = new StringBuffer(2048);
            buf.append("Retrieved runs");

            int i = 0;

            for (List<Run> runList : runs) {
                buf.append("\nCol " + i++ + " = " + runList.size());
            }

            logger.fine(buf.toString());
        }
    }
}
