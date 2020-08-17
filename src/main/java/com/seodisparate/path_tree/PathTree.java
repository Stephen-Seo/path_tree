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

public class PathTree implements Serializable {
    private static final long serialVersionUID = 1L;

    private class PathNode {
        String segment;
        String full_path;
        Map<String, PathNode> inner;
        public PathNode(String segment, String full_path) {
            this.segment = segment;
            this.full_path = full_path;
            inner = new HashMap<String, PathNode>();
        }

        public void prefix(Consumer<PathNode> action) {
            action.accept(this);
            for(PathNode node : inner.values()) {
                node.prefix(action);
            }
        }

        public void postfix(Consumer<PathNode> action) {
            for(PathNode node : inner.values()) {
                node.postfix(action);
            }
            action.accept(this);
        }

        public String getSegment() {
            return segment;
        }

        public String getFullPath() {
            return full_path;
        }

        public boolean put(String full_path) {
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
                        return true;
                    } else {
                        return inner.get(segment).put(full_path);
                    }
                }
            } else {
                return false;
            }
        }

        public boolean has(String full_path) {
            if(full_path == this.full_path) {
                return true;
            } else if(full_path.startsWith(this.full_path)) {
                String sub = full_path.substring(this.full_path.length());
                while(sub.startsWith("/")) {
                    sub = sub.substring(1);
                }
                int index = sub.indexOf('/');
                String segment = index == -1 ? sub : sub.substring(0, index);
                if(inner.containsKey(segment)) {
                    return inner.get(segment).has(full_path);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        public boolean remove(String full_path) {
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

    public void leavesOnly(Consumer<String> action) {
        root.postfix((n) -> {
            if(n.inner.isEmpty()) {
                action.accept(n.full_path);
            }
        });
    }

    public boolean put(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return false;
            }
        }
        return root.put(full_path);
    }

    public boolean has(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return false;
            }
        }
        return root.has(full_path);
    }

    public boolean remove(String full_path) {
        while(full_path.endsWith("/")) {
            full_path = full_path.substring(0, full_path.length() - 1);
            if(full_path.length() == 0) {
                return false;
            }
        }
        return root.remove(full_path);
    }

    public void clear() {
        root = new PathNode("/", "/");
    }

    public void printList() {
        root.prefix((n) -> System.out.println(n.full_path));
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        Collection<String> leaves = new ArrayList<String>();
        leavesOnly((path) -> leaves.add(path));
        out.writeInt(leaves.size());
        for(String leaf : leaves) {
            out.writeUTF(leaf);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        clear();

        int size = in.readInt();
        for(int i = 0; i < size; ++i) {
            put(in.readUTF());
        }
    }

    private void readObjectNoData() throws ObjectStreamException {
        clear();
    }
}