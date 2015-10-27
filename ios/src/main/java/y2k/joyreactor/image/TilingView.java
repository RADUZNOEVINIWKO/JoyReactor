package y2k.joyreactor.image;

import org.robovm.apple.coreanimation.CALayer;
import org.robovm.apple.coreanimation.CATiledLayer;
import org.robovm.apple.coregraphics.*;
import org.robovm.apple.imageio.CGImageDestination;
import org.robovm.apple.imageio.CGImagePropertyPNGData;
import org.robovm.apple.uikit.UIGraphics;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIImageAsset;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.Method;
import org.robovm.rt.bro.annotation.ByVal;
import org.robovm.rt.bro.annotation.MachineSizedFloat;

import java.io.File;

/**
 * Created by y2k on 10/25/15.
 */
@CustomClass("TilingView")
public class TilingView extends UIView {

    private String imageName;

    public TilingView(String imageName, CGSize size) {
        super(new CGRect(CGPoint.Zero(), size));
        this.imageName = imageName;

        CATiledLayer tiledLayer = (CATiledLayer) getLayer();
        tiledLayer.setLevelsOfDetail(4);
    }

    @Override
    public void setContentScaleFactor(@MachineSizedFloat double v) {
        super.setContentScaleFactor(1);
    }

    @Override
    public void draw(@ByVal CGRect rect) {
        CGContext context = UIGraphics.getCurrentContext();
        double scale = context.getCTM().getA();

        CATiledLayer tiledLayer = (CATiledLayer) getLayer();
        CGSize tileSize = tiledLayer.getTileSize();

        tileSize.setWidth(tileSize.getWidth() / scale);
        tileSize.setHeight(tileSize.getHeight() / scale);

        int firstCol = (int) Math.floor(rect.getMinX() / tileSize.getWidth());
        int lastCol = (int) Math.floor((rect.getMaxX() - 1) / tileSize.getWidth());
        int firstRow = (int) Math.floor(rect.getMinY() / tileSize.getHeight());
        int lastRow = (int) Math.floor((rect.getMaxY() - 1) / tileSize.getHeight());

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {

                UIImage tile = tileForScale(scale, row, col);
                CGRect tileRect = new CGRect(
                        tileSize.getWidth() * col, tileSize.getHeight() * row,
                        tileSize.getWidth(), tileSize.getHeight());
                tileRect = getBounds().intersection(tileRect);
                tile.draw(tileRect);
            }
        }
    }

    private UIImage tileForScale(double scale, int row, int col) {
        System.out.println("tileForScale | " + scale + " | " + row + " | " + col);

        CGRect rect = new CGRect();
        rect.setWidth(256);
        rect.setHeight(256);
        rect.setX(col * 256 / scale);
        rect.setY(row * 256 / scale);

        CGImage original = new UIImage(new File(imageName)).getCGImage();
        UIImage uiImage = new UIImage(CGImage.createWithImageInRect(original, rect));
        return new UIImage(uiImage.toPNGData());
    }

    @Method(selector = "layerClass")
    public static Class<? extends CALayer> getLayerClass() {
        return CATiledLayer.class;
    }
}