/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.version;

import org.apache.ignite.*;
import org.apache.ignite.internal.processors.cache.*;

/**
 * Cache version conflict resolver.
 */
public class CacheVersionConflictResolver {
    /**
     * Resolve the conflict.
     *
     * @param oldEntry Old entry.
     * @param newEntry New entry.
     * @param atomicVerComparator Whether to use atomic version comparator.
     * @return Conflict resolution context.
     * @throws IgniteCheckedException If failed.
     */
    public <K, V> GridCacheVersionConflictContext<K, V> resolve(GridCacheVersionedEntryEx<K, V> oldEntry,
        GridCacheVersionedEntryEx<K, V> newEntry, boolean atomicVerComparator) throws IgniteCheckedException {
        GridCacheVersionConflictContext<K, V> ctx = new GridCacheVersionConflictContext<>(oldEntry, newEntry);

        resolve0(ctx, oldEntry, newEntry, atomicVerComparator);

        return ctx;
    }

    /**
     * Internal conflict resolution routine.
     *
     * @param ctx Context.
     * @param oldEntry Old entry.
     * @param newEntry New entry.
     * @param atomicVerComparator Whether to use atomic version comparator.
     * @throws IgniteCheckedException If failed.
     */
    protected <K, V> void resolve0(GridCacheVersionConflictContext<K, V> ctx,
        GridCacheVersionedEntryEx<K, V> oldEntry, GridCacheVersionedEntryEx<K, V> newEntry,
        boolean atomicVerComparator) throws IgniteCheckedException {
        if (newEntry.dataCenterId() != oldEntry.dataCenterId())
            ctx.useNew();
        else {
            if (oldEntry.isStartVersion())
                ctx.useNew();
            else {
                if (atomicVerComparator) {
                    // Handle special case when version check using ATOMIC cache comparator is required.
                    if (GridCacheMapEntry.ATOMIC_VER_COMPARATOR.compare(oldEntry.version(), newEntry.version(), false) >= 0)
                        ctx.useOld();
                    else
                        ctx.useNew();
                }
                else {
                    long topVerDiff = newEntry.topologyVersion() - oldEntry.topologyVersion();

                    if (topVerDiff > 0)
                        ctx.useNew();
                    else if (topVerDiff < 0)
                        ctx.useOld();
                    else if (newEntry.order() > oldEntry.order())
                        ctx.useNew();
                    else
                        ctx.useOld();
                }
            }
        }
    }
}
