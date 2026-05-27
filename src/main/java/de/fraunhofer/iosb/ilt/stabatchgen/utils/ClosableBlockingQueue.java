/*
 * Copyright (C) 2026 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.stabatchgen.utils;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A blocking queue that offers its iterator as a way to take elements.
 *
 * @param <T> The type of the items in the queue.
 */
public class ClosableBlockingQueue<T> implements Iterable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClosableBlockingQueue.class.getName());

    private final Object[] items;
    private int count;
    private int putPtr;
    private int takePtr;
    private boolean open = true;
    private final ReentrantLock lock;
    private final Condition canTake;
    private final Condition canPut;
    private Iterator<T> iterator;

    public ClosableBlockingQueue(int capacity) {
        items = new Object[capacity];
        lock = new ReentrantLock();
        canTake = lock.newCondition();
        canPut = lock.newCondition();
    }

    public void close() throws InterruptedException {
        final ReentrantLock myLock = this.lock;
        myLock.lockInterruptibly();
        try {
            open = false;
            canTake.signalAll();
        } finally {
            myLock.unlock();
        }
    }

    public void put(T item) throws InterruptedException {
        Objects.requireNonNull(item);
        final ReentrantLock myLock = this.lock;
        myLock.lockInterruptibly();
        try {
            if (!open) {
                throw new IllegalStateException("Putting on a closed queue.");
            }
            while (count == items.length) {
                canPut.await();
            }
            final Object[] myItems = this.items;
            myItems[putPtr] = item;
            if (++putPtr == myItems.length) {
                putPtr = 0;
            }
            count++;
            canTake.signal();
        } finally {
            myLock.unlock();
        }
    }

    public T take() throws InterruptedException {
        final ReentrantLock myLock = this.lock;
        myLock.lockInterruptibly();
        try {
            while (count == 0 && open) {
                canTake.await();
            }
            if (count == 0) {
                // Nothing to take anymore.
                return null;
            }
            final Object[] myItems = this.items;
            @SuppressWarnings("unchecked")
            T item = (T) myItems[takePtr];
            myItems[takePtr] = null;
            if (++takePtr == myItems.length) {
                takePtr = 0;
            }
            count--;
            canPut.signal();
            return item;
        } finally {
            myLock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        if (iterator == null) {
            iterator = new Iterator<T>() {
                T next;

                @Override
                public boolean hasNext() {
                    if (next == null) {
                        try {
                            next = take();
                        } catch (InterruptedException ex) {
                            LOGGER.info("Woken up", ex);
                            // Interrupted, queue is closed.
                        }
                    }
                    return next != null;
                }

                @Override
                public T next() {
                    var myNext = next;
                    next = null;
                    return myNext;
                }
            };
        }
        return iterator;
    }

}
