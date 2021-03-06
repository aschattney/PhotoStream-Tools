/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hochschuledarmstadt.photostream_tools;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import hochschuledarmstadt.photostream_tools.model.Comment;
import hochschuledarmstadt.photostream_tools.model.Photo;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.thread.EventThread;

class AndroidSocket {

    public static final String NEW_PHOTO = "new_photo";
    private static final String TAG = AndroidSocket.class.getName();
    private static final String NEW_COMMENT = "new_comment";
    private static final String COMMENT_DELETED = "comment_deleted";
    private static final String PHOTO_DELETED = "photo_deleted";
    private static final String NEW_COMMENT_COUNT = "new_comment_count";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private final HttpImageLoader imageLoader;
    private final ImageCacher imageCacher;
    private IO.Options options;
    private Socket socket;
    private URI uri;
    private OnMessageListener onMessageListener;

    public AndroidSocket(IO.Options options, URI uri, HttpImageLoader imageLoader, ImageCacher imageCacher, OnMessageListener onMessageListener) throws NoSuchAlgorithmException, KeyManagementException {
        this.options = options;
        this.uri = uri;
        this.imageLoader = imageLoader;
        this.imageCacher = imageCacher;
        this.onMessageListener = onMessageListener;
    }

    public static SSLContext createSslContext() throws KeyManagementException, NoSuchAlgorithmException {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        TrustManager tm = new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[]{tm}, null);

        return sslContext;
    }

    public boolean connect() throws URISyntaxException {
        destroy();
        socket = IO.socket(uri, options);
        initializeSocket();
        socket.connect();
        return socket.connected();
    }

    private boolean disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
        return socket == null || !socket.connected();
    }

    public void destroy(){
        disconnect();
        if (socket != null) {
            socket.off();
            socket.close();
            socket = null;
        }
    }

    private void initializeSocket() {

        /*socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // onMessageListener.onError();
            }
        });


        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // onMessageListener.onConnectError(uri);
            }
        });

        socket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // onMessageListener.onConnectError(uri);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessageListener.onDisconnect();
            }
        });*/

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessageListener.onConnect();
            }
        });

        socket.on(NEW_PHOTO, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final Photo photo = new Gson().fromJson(args[0].toString(), Photo.class);
                ArrayList<Photo> list = new ArrayList<>(1);
                list.add(photo);
                imageLoader.execute(list);
                HttpImageLoader.HttpImage httpImage = imageLoader.take();
                if (httpImage != null) {
                    try {
                        imageCacher.cacheImage(httpImage.getPhoto(), httpImage.getImageData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onMessageListener.onNewPhoto(photo);
                        }
                    });
                }
                list.clear();
            }
        });

        socket.on(NEW_COMMENT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject jsonObject = (JSONObject) args[0];
                Gson gson = new Gson();
                final Comment comment = gson.fromJson(jsonObject.toString(), Comment.class);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onMessageListener.onNewComment(comment);
                    }
                });
            }
        });

        socket.on(COMMENT_DELETED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final int commentId = Integer.parseInt(args[0].toString());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onMessageListener.onCommentDeleted(commentId);
                    }
                });
            }
        });

        socket.on(PHOTO_DELETED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final int photoId = Integer.parseInt(args[0].toString());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onMessageListener.onPhotoDeleted(photoId);
                    }
                });
            }
        });

        socket.on(NEW_COMMENT_COUNT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject jsonObject = (JSONObject) args[0];
                try {
                    final int photoId = jsonObject.getInt("photo_id");
                    final int comment_count = jsonObject.getInt("comment_count");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onMessageListener.onCommentCountChanged(photoId, comment_count);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    interface OnMessageListener {
        void onNewPhoto(Photo photo);

        void onNewComment(Comment comment);

        void onCommentDeleted(int commentId);

        void onPhotoDeleted(int photoId);

        void onConnect();

        void onDisconnect();

        void onCommentCountChanged(int photoId, int comment_count);
    }
}
