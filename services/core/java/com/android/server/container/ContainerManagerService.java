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

package com.android.server.container;

import android.util.Slog;

import java.util.ArrayList;
import java.io.IOException;

public class ContainerManagerService    {
    static final String TAG = "ContainerManager";
    static final boolean DEBUG = false;

    
    final private static ArrayList<ContainerStateChangeListener> listeners =
        new ArrayList<ContainerStateChangeListener>();

    private static int mContainerId = 0; 
    private static boolean mInFocusedContainer;
    private static int mCurrentFocusedContainerId = 0; 

    public static void addContainerStateChangeListener(ContainerStateChangeListener listener)    {
        synchronized(listeners)    {
            listeners.add(listener);
        }
    }

    static void removeContainerStateChangeListener(ContainerStateChangeListener listener)    {
        synchronized(listeners)    {
            listeners.remove(listener);
        }
    }

    public static boolean isInFocusedContainer()    {    
        //return mInFocusedContainer;
	return (getContainerId() == getCurrentFocusedContainerId());
    }    

    public static int getContainerId()    {    
        return android.os.SystemProperties.getInt("ro.container.id", 0);
    } //modify by moto

    public static int getCurrentFocusedContainerId()    {
        String path = "/proc/container/active";
        int id = 0;

        java.io.BufferedReader br = null;
        try {
            br = new java.io.BufferedReader(new java.io.FileReader(path));
            String line = br.readLine();

            if(line == null)    return 0;

            try    {
                id = Integer.parseInt(line);
            } catch(NumberFormatException nfe)    {
                return 0; // do nothing here
            }
        } catch (IOException e) {
            return 0;
        } finally {
            try { if (br != null)    br.close(); } catch (IOException ex) {}
        }

        return id;
    }

    private final static android.os.UEventObserver mUEventObserver = new android.os.UEventObserver() {
        @Override
        public void onUEvent(android.os.UEventObserver.UEvent event) {
            Slog.i(TAG, "ACTIVE_CONTAINER_CHANGED: " + event);

            try    {
                mCurrentFocusedContainerId = Integer.parseInt(event.get("ACTIVE_CONTAINER_CHANGED"));
            } catch(NumberFormatException nfe)    {
                return; // do nothing here
            }

            if(mInFocusedContainer)    {
                if(mCurrentFocusedContainerId != mContainerId)    {
                    mInFocusedContainer = false;
                } else    {
                    // do nothing here
                }
            } else    {
                if(mCurrentFocusedContainerId == mContainerId)    {    
                    mInFocusedContainer = true; 
                } else    {
                    // do nothing here
                }
            }

	    synchronized(listeners)    {
                for(int i = 0; i < listeners.size(); i++)    {
                    ContainerStateChangeListener listener = listeners.get(i);
                    listener.containerFocusChanged(mCurrentFocusedContainerId);
                }
	    }
        }
    };

    /** Android static initialization is weird ...
    static    {
        mContainerId = getContainerId();
        mCurrentFocusedContainerId = getCurrentFocusedContainerId();
        mInFocusedContainer = (mCurrentFocusedContainerId == mContainerId) ? true : false;

	mUEventObserver.startObserving("ACTIVE_CONTAINER_CHANGED=");

	Slog.d(TAG, "static initializer: containerId=" + mContainerId +
	       ", mCurrentFocusedContainerId=" + mCurrentFocusedContainerId);

    }
     */
}

