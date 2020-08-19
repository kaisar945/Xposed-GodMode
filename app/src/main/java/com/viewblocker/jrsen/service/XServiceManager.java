package com.viewblocker.jrsen.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.ArrayMap;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class XServiceManager {

    private static final String TAG = "XServiceManager";
    private static final String DELEGATE_SERVICE = "clipboard";
    private static final Map<String, ServiceFetcher<?>> SERVICE_FETCHERS = new ArrayMap<String, ServiceFetcher<?>>();
    private static final HashMap<String, IBinder> sCache = new HashMap<String, IBinder>();
    private static final Method currentActivityThread;
    private static final Method getSystemContext;
    private static final Method checkService;

    private static final java.lang.String DESCRIPTOR = XServiceManager.class.getName();
    private static final int TRANSACTION_getService = IBinder.LAST_CALL_TRANSACTION;


    static {
        try {
            @SuppressLint("PrivateApi") Class<?> ActivityThreadClass = Class.forName("android.app.ActivityThread");
            currentActivityThread = ActivityThreadClass.getMethod("currentActivityThread");
            getSystemContext = ActivityThreadClass.getMethod("getSystemContext");
            @SuppressLint("PrivateApi") Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
            checkService = ServiceManagerClass.getMethod("checkService", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new UnsupportedOperationException("can not access ServiceManager#checkService method!", e);
        }
    }

    public interface ServiceFetcher<T> {
        T createService(Context ctx);
    }

    public static <T> void initManager() {
        try {
            @SuppressLint("PrivateApi") Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
            @SuppressLint("DiscouragedPrivateApi") Method getIServiceManagerMethod = ServiceManagerClass.getDeclaredMethod("getIServiceManager");
            getIServiceManagerMethod.setAccessible(true);
            final Object serviceManager = getIServiceManagerMethod.invoke(null);
            Field sServiceManagerField = ServiceManagerClass.getDeclaredField("sServiceManager");
            sServiceManagerField.setAccessible(true);
            Class<?> IServiceManagerClass = sServiceManagerField.getType();
            Object serviceManagerDelegate = Proxy.newProxyInstance(IServiceManagerClass.getClassLoader(), new Class[]{IServiceManagerClass}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    final String methodName = method.getName();
                    if ("addService".equals(methodName) && DELEGATE_SERVICE.equals(args[0])) {
                        IBinder systemService = (IBinder) args[1];
                        args[1] = new BinderDelegateService(systemService, new XService());
                        Object activityThread = currentActivityThread.invoke(null);
                        Context context = (Context) getSystemContext.invoke(activityThread);
                        for (Map.Entry<String, ServiceFetcher<?>> serviceFetcherEntry : SERVICE_FETCHERS.entrySet()) {
                            String name = serviceFetcherEntry.getKey();
                            IBinder service = (IBinder) serviceFetcherEntry.getValue().createService(context);
                            addService(name, service);
                        }
                    }
                    return method.invoke(serviceManager, args);
                }
            });
            sServiceManagerField.set(null, serviceManagerDelegate);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            Log.e(TAG, "init XServiceManager occur error", e);
        }
    }

    private static final class BinderDelegateService extends Binder {

        private final IBinder systemService;
        private final IBinder customService;

        public BinderDelegateService(IBinder systemService, IBinder customService) {
            this.systemService = systemService;
            this.customService = customService;
        }

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            return systemService.transact(code, data, reply, flags) || customService.transact(code, data, reply, flags);
        }
    }

    private static final class XService extends Binder {

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            java.lang.String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_getService: {
                    data.enforceInterface(descriptor);
                    String name = data.readString();
                    reply.writeNoException();
                    IBinder binder = sCache.get(name);
                    reply.writeStrongBinder(binder);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

    }

    public static <T> void registerService(String name, ServiceFetcher<T> serviceFetcher) {
        SERVICE_FETCHERS.put(name, serviceFetcher);
    }

    public static void addService(String name, IBinder service) {
        sCache.put(name, service);
    }

    /**
     * Returns a reference to a service with the given name.
     *
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     */
    public static IBinder getService(String name) {
        try {
            IBinder service = (IBinder) checkService.invoke(null, DELEGATE_SERVICE);
            Objects.requireNonNull(service, "can't not access delegate service");
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            try {
                _data.writeInterfaceToken(DESCRIPTOR);
                _data.writeString(name);
                service.transact(TRANSACTION_getService, _data, _reply, 0);
                _reply.readException();
                return _reply.readStrongBinder();
            } finally {
                _data.recycle();
                _reply.recycle();
            }
        } catch (NullPointerException | IllegalAccessException | InvocationTargetException | RemoteException e) {
            Log.e(TAG, "error in getService", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <I extends IInterface> I getServiceInterface(String name) {
        try {
            Objects.requireNonNull(name, "service name is null");
            IBinder service = getService(name);
            Objects.requireNonNull(service, String.format("can't found %s service", name));
            String descriptor = service.getInterfaceDescriptor();
            Class<?> StubClass = Objects.requireNonNull(XServiceManager.class.getClassLoader()).loadClass(descriptor + "$Stub");
            return (I) StubClass.getMethod("asInterface", IBinder.class).invoke(null, service);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException | RemoteException e) {
            Log.e(TAG, "error in getService", e);
            return null;
        }
    }

}
