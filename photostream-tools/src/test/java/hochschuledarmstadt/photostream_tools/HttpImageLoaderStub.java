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

import java.nio.ByteBuffer;
import java.util.List;

import hochschuledarmstadt.photostream_tools.model.Photo;

public class HttpImageLoaderStub extends HttpImageLoader {

    private Photo photo;
    private byte[] bytes = ByteBuffer.allocate(4).putInt(0xffd8ffe0).array();
    private boolean isRunning = true;

    public HttpImageLoaderStub() {
        super(null);
    }

    @Override
    public void execute(List<Photo> photos) {
        photo = photos.get(0);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public HttpImage take() {
        isRunning = false;
        return new HttpImage(photo, bytes);
    }

    @Override
    public void onResponse(byte[] imageData, Photo photo, boolean fileNotFound) {
        super.onResponse(imageData, photo, fileNotFound);
    }
}
