package mods.battlegear2.client.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.item.ItemStack;

/**
 * User: nerd-boy
 * Date: 2/08/13
 * Time: 3:02 PM
 * TODO: Add discription
 */
public class ImageCache {
    public static int CACHE_SIZE = 25;
    private static ItemStack temp;

    private static final DynamicTexture defaultTexture;

    private static final DynamicTexture test1;

    static{
        defaultTexture = new DynamicTexture(ImageData.IMAGE_RES, ImageData.IMAGE_RES);

        test1 = new DynamicTexture(ImageData.IMAGE_RES, ImageData.IMAGE_RES);

        ImageData.defaultImage.setTexture(defaultTexture.getTextureData());
    }

    private static final LoadingCache<String, DynamicTexture> imageCache =
            CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).
                    build(
                            new CacheLoader<String, DynamicTexture>() {
                                @Override
                                public DynamicTexture load(String key) throws Exception {
                                    DynamicTexture texture = new DynamicTexture(ImageData.IMAGE_RES, ImageData.IMAGE_RES);

                                    return texture;
                                }

                            }
                    );
    static {
        CacheBuilder.newBuilder().build();
    }


    public static void setTexture(ItemStack stack){

        ImageData.defaultImage.setTexture(defaultTexture.getTextureData());
        defaultTexture.updateDynamicTexture();

    }

}
