// This is free and unencumbered software released into the public domain.
// 
// Anyone is free to copy, modify, publish, use, compile, sell, or
// distribute this software, either in source code form or as a compiled
// binary, for any purpose, commercial or non-commercial, and by any
// means.
// 
// In jurisdictions that recognize copyright laws, the author or authors
// of this software dedicate any and all copyright interest in the
// software to the public domain. We make this dedication for the benefit
// of the public at large and to the detriment of our heirs and
// successors. We intend this dedication to be an overt act of
// relinquishment in perpetuity of all present and future rights to this
// software under copyright law.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
// OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
// 
// For more information, please refer to <https://unlicense.org>

package com.seodisparate.path_tree;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.function.Consumer;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * The PathTree class helps to keep track of a directory structure. It implements Serializable.
 */
public class PathTree<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public class PathNode {
        private String segment;
        private String full_path;
        private Map<String, PathNode> inner;
        private T data;
        public PathNode(String segment, String full_path) {
            this.segment = segment;
            this.full_path = full_path;
            inner = new HashMap<String, PathNode>();
            data = null;
        }

        public PathNode(String segment, String full_path, T data) {
            this.segment = segment;
            this.full_path = full_path;
            this.data = data;
            inner = new HashMap<String, PathNode>();
        }

        /**
         * Does a "prefix" traversal of the tree with the current node as root.
         * @param action The function to call during a "prefix" traversal
         */
        public void prefix(Consumer<PathNode> action) {
            action.accept(this);
            for(PathNode node : inner.values()) {
                node.prefix(action);
            }
        }

        /**
         * Does a "postfix" traversal of the tree with the current node as
         * root.
         * @param action The function to call during a "postfix" traversal
         */
        public void postfix(Consumer<PathNode> action) {
            for(PathNode node : inner.values()) {
                node.postfix(action);
            }
            action.accept(this);
        }

        /**
         * Gets the segment string represented by this node
         * @return Returns the segment
         */
        public String getSegment() {
            return segment;
        }

        /**
         * Gets the full path represented by this node
         * @return Returns the full path
         */
        public String getFullPath() {
            return full_path;
        }

        /**
         * Gets the data associated with this node, or null if there is none.
         * @return The data associated with this node
         */
        public T getData() {
            return data;
        }

        /**
         * Sets the data associated with this node. Can be null.
         * @param data The data to store with this node
         */
        public void setData(T data) {
            this.data = data;
        }

        private boolean put(String full_path) {
            return put(full_path, null);
        }

        private boolean put(String full_path, T data) {
            if(full_path.startsWith(this.full_path)) {
                String sub = full_path.substring(this.full_path.length());
                while(sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                int index = sub.indexOf('/');
                String segment = index == -1 ? sub : sub.substring(0, index);
                //System.out.println("this.full_path = \"" + this.full_path + "\", sub = \"" + sub + "\", segment = \"" + segment + "\"");
                if(inner.containsKey(segment)) {
                    return inner.get(segment).put(full_path);
                } else {
                    String next_full_path = this.full_path;
                    if(next_full_path.endsWith("/")) {
                        next_full_path += segment;
                    } else {
                        next_full_path += "/" + segment;
                    }
                    PathNode newPathNode = new PathNode(segment, next_full_path);
                    inner.put(segment, newPathNode);
                    if(index == -1) {
                        newPathNode.data = data;
                        return true;
                    } else {
                        return inner.get(segment).put(full_path);
                    }
                }
            } else {
                return false;
            }
        }

        /**
         * Returns true if the full_path exists in the tree.
         * @param full_path The full path to check
         * @return True if the path exists
         */
        public boolean has(String full_path) {
            if(full_path.equals(this.full_path)) {
                return true;
            } else if(full_path.startsWith(this.full_path)) {
                String sub = full_path.substring(this.full_path.length());
                while(sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                int index = sub.indexOf('/');
                String segment = index == -1 ? sub : sub.substring(0, index);
                //System.out.println("full_path = \"" + full_path + "\", this.full_path = \"" + this.full_path + "\", sub = \"" + sub + "\", segment = \"" + segment + "\"");
                if(inner.containsKey(segment)) {
                    return inner.get(segment).has(full_path);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        /**
         * Returns the PathNode at the given full_path, or null if there is none
         * @param full_path The full path to check
         * @return A PathNode if it exists, or null
         */
        public PathNode get(String full_path) {
            if(full_path.equals(this.full_path)) {
                return this;
            } else if(full_path.startsWith(this.full_path)) {
                String sub = full_path.substring(this.full_path.length());
                while(sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                int index = sub.indexOf('/');
                String segment = index == -1 ? sub : sub.substring(0, index);
                //System.out.println("full_path = \"" + full_path + "\", this.full_path = \"" + this.full_path + "\", sub = \"" + sub + "\", segment = \"" + segment + "\"");
                if(inner.containsKey(segment)) {
                    return inner.get(segment).get(full_path);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        private boolean remove(String full_path) {
            if(full_path.startsWith(this.full_path)) {
                String sub = full_path.substring(this.full_path.length());
                while(sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                int index = sub.indexOf('/');
                String segment = index == -1 ? sub : sub.substring(0, index);
                //System.out.println("this.full_path = \"" + this.full_path + "\", sub = \"" + sub + "\", segment = \"" + segment + "\"");
                if(inner.containsKey(segment)) {
                    if(inner.get(segment).full_path.equals(full_path)) {
                        inner.remove(segment);
                        return true;
                    } else {
                        return inner.get(segment).remove(full_path);
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    PathNode root;

    public PathTree() {
        root = new PathNode("/", "/");
    }

    /**
     * Invokes "action" on leaves of the stored directory structure.
     * @param action The action to call with the full path of each leaf
     */
    public void leavesOnly(Consumer<String> action) {
        root.postfix((n) -> {
            if(n.inner.isEmpty()) {
                action.accept(n.full_path);
            }
        });
    }

    /**
     * Puts a path into the directory structure. Parent directories need not be
     * specified first, as this data structure will automatically create them
     * for you.
     * @param full_path The path to put in the directory structure
     * @return True if the path was created, false if it existed already or if
     * the path was invalid
     */
    public boolean put(String full_path) {
        return put(full_path, null);
    }

    /**
     * Puts a path into the directory structure with the associated data. Parent
     * directories need not be specified first, as this data structure will
     * automatically create them for you. Note that if the path already exists,
     * the data will not be set.
     * @param full_path The path to put in the directory structure
     * @param data The data to associate with the given path
     * @return True if the path was created and data stored
     */
    public boolean put(String full_path, T data) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return false;
            }
        }
        return root.put(full_path, data);
    }

    /**
     * Checks if a path exists in the directory structure.
     * @param full_path The path to check
     * @return True if the path exists
     */
    public boolean has(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return true;
            }
        }
        return root.has(full_path);
    }

    /**
     * Gets a PathNode at the given full_path, or null if it doesn't exist.
     * @param full_path The path to lookup
     * @return An existing PathNode instance, or null if there is no node at the
     * given path
     */
    public PathNode get(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return root;
            }
        }
        return root.get(full_path);
    }

    /**
     * Removes a path from the directory structure. Note that child paths will
     * also be removed.
     * @param full_path The path to remove
     * @return True if the path was found and was removed
     */
    public boolean remove(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return false;
            }
        }
        return root.remove(full_path);
    }

    /**
     * Resets PathTree to the initial state of only having a root directory.
     */
    public void clear() {
        root = new PathNode("/", "/");
    }

    /**
     * Prints all paths in the directory structure. Note this will only print
     * paths to each leaf.
     */
    public void printList() {
        leavesOnly((n) -> System.out.println(n));
    }

    // Function required for serialization
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        Collection<String> leaves = new ArrayList<String>();
        leavesOnly((path) -> leaves.add(path));
        out.writeInt(leaves.size());
        for(String leaf : leaves) {
            out.writeUTF(leaf);
        }
    }

    // Function required for deserialization
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        clear();

        int size = in.readInt();
        for(int i = 0; i < size; ++i) {
            put(in.readUTF());
        }
    }

    // Function required for deserialization
    private void readObjectNoData() throws ObjectStreamException {
        clear();
    }
}