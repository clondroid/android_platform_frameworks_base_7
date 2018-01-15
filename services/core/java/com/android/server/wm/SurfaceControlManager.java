/*
 * Copyright (C) 2015-2017 The Android Container Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm;

import android.os.Debug;
import android.util.Slog;

import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.Surface.OutOfResourcesException;

import java.util.ArrayList;

/**
 * ContainerSurfaceControlManager
 *  @hide
 */
public class SurfaceControlManager {
    private static final String TAG = "SurfaceControlManager";
    private final static boolean logSurfaceTrace = false;
    private static int mContainerId = 0;
    private static boolean mInFocusedContainer = true;

    public static SurfaceControl createSurfaceControl(SurfaceSession s,
                                     String name, int w, int h, int format, int flags)    {
        //if(WindowManagerService.ENABLE_CONTAINER_LAYERING)
        //    return new ContainerSurfaceControl(s, name, w, h, format, flags);
        //else
            return new SurfaceControl(s, name, w, h, format, flags);
    }

    static void init(int id, boolean focused)    {
	mContainerId = id;
	mInFocusedContainer = focused;
    }

    static void setContainerFocused(boolean focused)    {
        mInFocusedContainer = focused;
    }
    
    static void requestContainerFocus()    {
	setContainerFocused(true);

        synchronized (sSurfaces) {
            for (int i = sSurfaces.size() - 1; i >= 0; i--) {
                ContainerSurfaceControl s = sSurfaces.get(i);
		s.mLayer |= 0x40000000;
                s.simpleSetLayer(s.mLayer);

                if (logSurfaceTrace)
		      Slog.d(TAG, "requestContainerFocus: \n    " + s);
            }
        }
    }    

    static void releaseContainerFocus()    {
	setContainerFocused(false);

        synchronized (sSurfaces) {
            for (int i = sSurfaces.size() - 1; i >= 0; i--) {
                ContainerSurfaceControl s = sSurfaces.get(i);
                s.mLayer &= 0x3FFFFFFF;
                s.simpleSetLayer(s.mLayer);

		if (logSurfaceTrace)
                    Slog.d(TAG, "releaseContainerFocus: \n    " + s); 
            }
        }
    }    

    private static ArrayList<ContainerSurfaceControl> sSurfaces = new ArrayList<ContainerSurfaceControl>();

    private static class ContainerSurfaceControl extends SurfaceControl {
	private int mLayer;
	private final String mName;
        
        public ContainerSurfaceControl(SurfaceSession s,
                                       String name, int w, int h, int format, int flags)
                   throws OutOfResourcesException {
            super(s, name, w, h, format, flags);
          
	    mName = name != null ? name : "Not named";
            synchronized (sSurfaces) {
                sSurfaces.add(this);
            }
        }

	@Override
	public synchronized void setLayer(int zorder) {
	    if(mInFocusedContainer)
                zorder += (0x40000000 + 100000000 * mContainerId);
	    else
		zorder += (100000000 * mContainerId);

	    simpleSetLayer(zorder);
	}

	synchronized void simpleSetLayer(int zorder)     {
	    mLayer = zorder;

            super.setLayer(zorder);
        }

        @Override
        public void destroy() {
            super.destroy();
            
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

        @Override
        public void release() {
            super.release();
            
            synchronized (sSurfaces) {
                sSurfaces.remove(this);
            }
        }

	@Override
	public String toString() {
	    return "Surface " + Integer.toHexString(System.identityHashCode(this)) + " "
		    + mName + " layer=" + mLayer;
	}
    }
}

