package org.oskari.cli.heightmap;

import org.oskari.wcs.geotiff.IFD;
import org.oskari.wcs.geotiff.TIFFReader;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class FloatGeoTIFF {
    
    private static final Logger LOG = LogFactory.getLogger(FloatGeoTIFF.class);
    
    private final TIFFReader r;
    private final IFD ifd;
    private final float[][] tiles;
    private final int tilesAcross;
    private final double tx;
    private final double sx;
    private final double ty;
    private final double sy;
    
    public FloatGeoTIFF(byte[] buf) {
        this.r = new TIFFReader(buf);
        this.ifd = r.getIFD(0);
        
        int tw = ifd.getTileWidth();
        int th = ifd.getTileHeight();
        
        int tilesAcross = ifd.getWidth() / tw;
        if (tilesAcross * tw < ifd.getWidth()) {
            tilesAcross++;
        }
        int tilesDown = ifd.getHeight() / th;
        if (tilesDown * th < ifd.getHeight()) {
            tilesDown++;
        }
        this.tilesAcross = tilesAcross;
        
        tiles = new float[ifd.getTileOffsets().length][tw * th];
        for (int i = 0; i < ifd.getTileOffsets().length; i++) {
            r.readTile(0, i, tiles[i]);
        }
        
        LOG.debug("Width:", ifd.getWidth(), "Height:", ifd.getHeight(),
                "TileWidth:", tw, "TileHeight:", th,
                "TilesAcross:", tilesAcross, "TilesDown:", tilesDown,
                "NumTiles:", ifd.getTileOffsets().length);
        
        
        this.tx = ifd.getModelTransformation()[3];
        this.sx = 1.0 / ifd.getModelTransformation()[0];
        this.ty = ifd.getModelTransformation()[7];
        this.sy = 1.0 / ifd.getModelTransformation()[5];
    }
    
    public float getValue(double e, double n) {
       int x = (int) Math.round(sx * (e - tx));
       int y = (int) Math.round(sy * (n - ty));
       if (x < 0 || x >= ifd.getWidth()) {
           LOG.debug("x:", x, "outside image", "width:", ifd.getWidth(), "e:", e, "tx:", tx);
           return Float.NaN;
       }
       if (y < 0 || y >= ifd.getHeight()) {
           LOG.debug("y:", y, "outside image", "height:", ifd.getHeight(), "n:", n, "ty:", ty);
           return Float.NaN;
       }
       return getValue(x, y);
    }
    
    private float getValue(int x, int y) {
        int tileX = x / ifd.getTileWidth();
        int offX = x % ifd.getTileWidth();
        int tileY = y / ifd.getTileHeight();
        int offY = y % ifd.getTileHeight();
        int tileIndex = tileY * tilesAcross + tileX;
        int tileOffset = offY * ifd.getTileWidth() + offX;
        try {
            return tiles[tileIndex][tileOffset];
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.warn("Tile X:", tileX, "Off X", offX,
                    "Tile Y:", tileY, "Off Y", offY,
                    "TileIndex:", tileIndex, "TileOffset:", tileOffset);
            return 0.0f;
        }
    }
    
    public double getLowerLeftEast() {
        return ifd.getModelTransformation()[4];
    }
    
    public double getLowerLeftNorth() {
        return ifd.getModelTransformation()[5];
    }
    

}
