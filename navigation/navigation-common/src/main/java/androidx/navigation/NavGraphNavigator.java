/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Navigator built specifically for {@link NavGraph} elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 */
@Navigator.Name("navigation")
public class NavGraphNavigator extends Navigator<NavGraph> {
    private final NavigatorProvider mNavigatorProvider;

    /**
     * Construct a Navigator capable of routing incoming navigation requests to the proper
     * destination within a {@link NavGraph}.
     *
     * @param navigatorProvider NavigatorProvider used to retrieve the correct
     *                          {@link Navigator} to navigate to the start destination
     */
    public NavGraphNavigator(@NonNull NavigatorProvider navigatorProvider) {
        mNavigatorProvider = navigatorProvider;
    }

    /**
     * Creates a new {@link NavGraph} associated with this navigator.
     * @return
     */
    @NonNull
    @Override
    public NavGraph createDestination() {
        return new NavGraph(this);
    }

    @Nullable
    @Override
    public NavDestination navigate(@NonNull NavGraph destination, @Nullable Bundle args,
            @Nullable NavOptions navOptions, @Nullable Extras navigatorExtras) {
        int startId = destination.getStartDestination();
        if (startId == 0) {
            throw new IllegalStateException("no start destination defined via"
                    + " app:startDestination for "
                    + destination.getDisplayName());
        }
        NavDestination startDestination = destination.findNode(startId, false);
        if (startDestination == null) {
            final String dest = destination.getStartDestDisplayName();
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is not a direct child of this NavGraph");
        }
        Navigator<NavDestination> navigator = mNavigatorProvider.getNavigator(
                startDestination.getNavigatorName());
        return navigator.navigate(startDestination, startDestination.addInDefaultArgs(args),
                navOptions, navigatorExtras);
    }

    @Override
    public boolean popBackStack() {
        return true;
    }
}
