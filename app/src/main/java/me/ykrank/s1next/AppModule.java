package me.ykrank.s1next;

import android.content.Context;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ykrank.androidtools.widget.EditorDiskCache;
import com.github.ykrank.androidtools.widget.PersistentHttpCookieStore;
import com.github.ykrank.androidtools.widget.RxBus;
import com.github.ykrank.androidtools.widget.hostcheck.HttpDns;
import com.github.ykrank.androidtools.widget.track.DataTrackAgent;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.rx_cache2.internal.RxCache;
import io.victoralbertos.jolyglot.JacksonSpeaker;
import me.ykrank.s1next.data.User;
import me.ykrank.s1next.data.Wifi;
import me.ykrank.s1next.data.api.Api;
import me.ykrank.s1next.data.api.ApiCacheProvider;
import me.ykrank.s1next.data.api.ApiVersionInterceptor;
import me.ykrank.s1next.data.api.S1Service;
import me.ykrank.s1next.data.api.UserValidator;
import me.ykrank.s1next.data.api.app.AppApi;
import me.ykrank.s1next.data.api.app.AppService;
import me.ykrank.s1next.data.api.app.AppTokenInterceptor;
import me.ykrank.s1next.data.pref.AppDataPreferencesManager;
import me.ykrank.s1next.data.pref.DownloadPreferencesManager;
import me.ykrank.s1next.data.pref.NetworkPreferencesManager;
import me.ykrank.s1next.task.AutoSignTask;
import me.ykrank.s1next.viewmodel.UserViewModel;
import me.ykrank.s1next.widget.RawJsonConverterFactory;
import me.ykrank.s1next.widget.glide.AvatarUrlsCache;
import me.ykrank.s1next.widget.glide.OkHttpNoAvatarInterceptor;
import me.ykrank.s1next.widget.hostcheck.AppHostUrl;
import me.ykrank.s1next.widget.hostcheck.AppMultiHostInterceptor;
import me.ykrank.s1next.widget.hostcheck.NoticeCheckTask;
import me.ykrank.s1next.widget.net.AppData;
import me.ykrank.s1next.widget.net.Data;
import me.ykrank.s1next.widget.net.Image;
import okhttp3.CookieJar;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Provides instances of the objects when we need to inject.
 */
@Module(includes = BuildTypeModule.class)
public final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        this.mApp = app;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return mApp;
    }

    @Provides
    @Singleton
    AppHostUrl provideBaseHostUrl(NetworkPreferencesManager networkPreferencesManager) {
        return new AppHostUrl(networkPreferencesManager);
    }

    @Provides
    @Singleton
    HttpDns provideHttpDns(AppHostUrl baseHostUrl) {
        return new HttpDns(baseHostUrl);
    }

    @Provides
    @Singleton
    CookieManager providerCookieManager(Context context) {
        return new CookieManager(new PersistentHttpCookieStore(context), CookiePolicy.ACCEPT_ALL);
    }

    @Provides
    @Singleton
    CookieJar providerCookieJar(CookieManager cookieManager) {
        return new JavaNetCookieJar(cookieManager);
    }

    @Data
    @Provides
    @Singleton
    OkHttpClient.Builder providerDataOkHttpClientBuilder(CookieJar cookieJar, AppHostUrl baseHostUrl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.dns(new HttpDns(baseHostUrl));
        builder.connectTimeout(10, TimeUnit.SECONDS);
        builder.writeTimeout(20, TimeUnit.SECONDS);
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        builder.cookieJar(cookieJar);
        builder.addInterceptor(new ApiVersionInterceptor());
        builder.addInterceptor(new AppMultiHostInterceptor(baseHostUrl));

        return builder;
    }

    @AppData
    @Provides
    @Singleton
    OkHttpClient.Builder providerAppDataOkHttpClientBuilder(CookieJar cookieJar, User user) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(10, TimeUnit.SECONDS);
        builder.writeTimeout(20, TimeUnit.SECONDS);
        builder.readTimeout(10, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        builder.cookieJar(cookieJar);
        builder.addNetworkInterceptor(new AppTokenInterceptor(user));

        return builder;
    }

    @Image
    @Provides
    @Singleton
    OkHttpClient.Builder providerImageOkHttpClientBuilder(CookieJar cookieJar, AppHostUrl baseHostUrl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.dns(new HttpDns(baseHostUrl));
        builder.connectTimeout(17, TimeUnit.SECONDS);
        builder.writeTimeout(17, TimeUnit.SECONDS);
        builder.readTimeout(77, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        builder.cookieJar(cookieJar);
        builder.addNetworkInterceptor(new OkHttpNoAvatarInterceptor());
        builder.addInterceptor(new AppMultiHostInterceptor(baseHostUrl));

        return builder;
    }

    @Provides
    @Singleton
    S1Service providerRetrofit(@Data OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Api.BASE_API_URL)
                .addConverterFactory(RawJsonConverterFactory.Companion.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(S1Service.class);
    }

    @Provides
    @Singleton
    AppService providerAppRetrofit(@AppData OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(AppApi.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(AppService.class);
    }

    @Provides
    @Singleton
    ApiCacheProvider providerApiCacheProvider(Context context, DownloadPreferencesManager downloadPreferencesManager) {
        String cachePath = context.getCacheDir().getAbsolutePath() + "/rx_cache";
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return new RxCache.Builder()
                .useExpiredDataIfLoaderNotAvailable(true)
                .setMaxMBPersistenceCache(downloadPreferencesManager.getTotalDataCacheSize())
                .persistence(new File(cachePath), new JacksonSpeaker())
                .using(ApiCacheProvider.class);
    }

    @Provides
    @Singleton
    Wifi providerWifi() {
        return new Wifi();
    }

    @Provides
    @Singleton
    ObjectMapper provideJsonObjectMapper() {
        return new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
                .configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
    }

    @Provides
    @Singleton
    RxBus providerRxBus() {
        return new RxBus();
    }

    @Provides
    @Singleton
    User providerUser(UserViewModel userViewModel) {
        return userViewModel.getUser();
    }

    @Provides
    @Singleton
    AutoSignTask provideAutoSignTask(S1Service s1Service, User user) {
        return new AutoSignTask(s1Service, user);
    }

    @Provides
    @Singleton
    UserValidator providerUserValidator(User user, AutoSignTask autoSignTask) {
        return new UserValidator(user, autoSignTask);
    }

    @Provides
    @Singleton
    UserViewModel providerUserViewModel(AppDataPreferencesManager appDataPreferencesManager) {
        return new UserViewModel(appDataPreferencesManager);
    }


    @Provides
    @Singleton
    DataTrackAgent provideDataTrackAgent() {
        return new DataTrackAgent();
    }

    @Provides
    @Singleton
    NoticeCheckTask provideNoticeCheckTask(RxBus rxBus, S1Service s1Service, User user) {
        return new NoticeCheckTask(rxBus, s1Service, user);
    }

    @Provides
    @Singleton
    EditorDiskCache provideEditorDiskCache(Context context) {
        return new EditorDiskCache(context.getCacheDir().getPath()
                + File.separator + "editor_disk_cache");
    }

    @Provides
    @Singleton
    AvatarUrlsCache provideAvatarUrlsCache() {
        return new AvatarUrlsCache();
    }
}
