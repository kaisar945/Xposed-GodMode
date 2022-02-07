package com.kaisar.xposed.godmode.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.signature.ObjectKey;
import com.kaisar.xposed.godmode.injection.bridge.GodModeManager;
import com.kaisar.xposed.godmode.rule.ViewRule;

import java.io.FileNotFoundException;

@GlideModule
public class GmGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(ViewRule.class, Bitmap.class, new RuleModelLoaderFactory());
    }

    static class RuleModelLoaderFactory implements ModelLoaderFactory<ViewRule, Bitmap> {

        @NonNull
        @Override
        public ModelLoader<ViewRule, Bitmap> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new RuleModelLoader();
        }

        @Override
        public void teardown() {
        }
    }

    static class RuleModelLoader implements ModelLoader<ViewRule, Bitmap> {

        @Override
        public LoadData<Bitmap> buildLoadData(@NonNull ViewRule viewRule, int width, int height, @NonNull Options options) {
            return new LoadData<>(new ObjectKey(viewRule), new RuleDataFetcher(viewRule));
        }

        @Override
        public boolean handles(@NonNull ViewRule viewRule) {
            return true;
        }
    }

    static class RuleDataFetcher implements DataFetcher<Bitmap> {

        final ViewRule mViewRule;

        public RuleDataFetcher(ViewRule viewRule) {
            mViewRule = viewRule;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
            Bitmap bitmap = GodModeManager.getDefault().openImageFileBitmap(mViewRule.imagePath);
            if (bitmap != null) {
                try {
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, mViewRule.x, mViewRule.y, mViewRule.width, mViewRule.height);
                    callback.onDataReady(croppedBitmap);
                } finally {
                    bitmap.recycle();
                }
            } else {
                callback.onLoadFailed(new FileNotFoundException(mViewRule.imagePath));
            }
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void cancel() {
        }

        @NonNull
        @Override
        public Class<Bitmap> getDataClass() {
            return Bitmap.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
