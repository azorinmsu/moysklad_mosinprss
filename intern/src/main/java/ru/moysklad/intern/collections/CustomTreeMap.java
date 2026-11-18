package ru.moysklad.intern.collections;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import ru.moysklad.intern.collections.map.AbstractCustomMap;

public class CustomTreeMap<K, V> implements AbstractCustomMap<K, V> {

    private static final boolean RED = false;
    private static final boolean BLACK = true;

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;
        Node<K, V> parent;
        boolean color = BLACK;

        Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
    }

    private Node<K, V> root;
    private int size = 0;
    private final Comparator<? super K> comparator;

    public CustomTreeMap() {
        this.comparator = null;
    }

    public CustomTreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int compare(Object k1, Object k2) {
        if (comparator != null) {
            return comparator.compare((K) k1, (K) k2);
        }
        return ((Comparable<? super K>) k1).compareTo((K) k2);
    }

    @Override
    public V put(K key, V val) {
        if (key == null) {
            throw new NullPointerException("Ключ не может быть null");
        }

        if (root == null) {
            root = new Node<>(key, val, null);
            root.color = BLACK;
            size = 1;
            return null;
        }

        Node<K, V> current = root;
        Node<K, V> parent;
        int cmp;

        do {
            parent = current;
            cmp = compare(key, current.key);
            if (cmp < 0) {
                current = current.left;
            } else if (cmp > 0) {
                current = current.right;
            } else {
                V oldVal = current.value;
                current.value = val;
                return oldVal;
            }
        } while (current != null);

        Node<K, V> newNode = new Node<>(key, val, parent);
        newNode.color = RED;

        if (cmp < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        fixAfterInsertion(newNode);
        size++;
        return null;
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = getNode(key);
        return node == null ? null : node.value;
    }

    @Override
    public boolean containsKey(Object key) {
        return getNode(key) != null;
    }

    private Node<K, V> getNode(Object key) {
        if (key == null) {
            return null;
        }
        Node<K, V> current = root;
        while (current != null) {
            int cmp = compare(key, current.key);
            if (cmp < 0) {
                current = current.left;
            } else if (cmp > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        Node<K, V> p = getNode(key);
        if (p == null) {
            return null;
        }
        V oldValue = p.value;
        deleteNode(p);
        return oldValue;
    }

    public K firstKey() {
        if (root == null) {
            throw new NoSuchElementException("Дерево пусто");
        }
        Node<K, V> p = root;
        while (p.left != null) {
            p = p.left;
        }
        return p.key;
    }

    public K lastKey() {
        if (root == null) {
            throw new NoSuchElementException("Дерево пусто");
        }
        Node<K, V> p = root;
        while (p.right != null) {
            p = p.right;
        }
        return p.key;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    // Левый поворот вокруг узла p
    private void rotateLeft(Node<K, V> p) {
        if (p != null) {
            Node<K, V> r = p.right;
            p.right = r.left;
            if (r.left != null) {
                r.left.parent = p;
            }
            r.parent = p.parent;
            if (p.parent == null) {
                root = r;
            } else if (p.parent.left == p) {
                p.parent.left = r;
            } else {
                p.parent.right = r;
            }
            r.left = p;
            p.parent = r;
        }
    }

    // Правый поворот вокруг узла p
    private void rotateRight(Node<K, V> p) {
        if (p != null) {
            Node<K, V> l = p.left;
            p.left = l.right;
            if (l.right != null) {
                l.right.parent = p;
            }
            l.parent = p.parent;
            if (p.parent == null) {
                root = l;
            } else if (p.parent.right == p) {
                p.parent.right = l;
            } else {
                p.parent.left = l;
            }
            l.right = p;
            p.parent = l;
        }
    }

    // Балансировка красно-чёрного дерева после вставки
    private void fixAfterInsertion(Node<K, V> x) {
        x.color = RED;

        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Node<K, V> y = rightOf(parentOf(parentOf(x))); // дядя
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<K, V> y = leftOf(parentOf(parentOf(x))); // дядя
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    // Удаление узла из дерева
    private void deleteNode(Node<K, V> p) {
        size--;

        // Если у узла два потомка — заменяем его на successor (следующий по значению)
        if (p.left != null && p.right != null) {
            Node<K, V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        }

        Node<K, V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                p.parent.left = replacement;
            } else {
                p.parent.right = replacement;
            }

            p.left = p.right = p.parent = null;

            if (p.color == BLACK) {
                fixAfterDeletion(replacement);
            }
        } else if (p.parent == null) {
            root = null;
        } else {
            if (p.color == BLACK) {
                fixAfterDeletion(p);
            }
            if (p.parent != null) {
                if (p == p.parent.left) {
                    p.parent.left = null;
                } else if (p == p.parent.right) {
                    p.parent.right = null;
                }
                p.parent = null;
            }
        }
    }

    // Балансировка красно-чёрного дерева после удаления
    private void fixAfterDeletion(Node<K, V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Node<K, V> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else {
                Node<K, V> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        setColor(x, BLACK);
    }

    private Node<K, V> successor(Node<K, V> t) {
        if (t == null) {
            return null;
        } else if (t.right != null) {
            Node<K, V> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            Node<K, V> p = t.parent;
            Node<K, V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    private static <K, V> boolean colorOf(Node<K, V> p) {
        return p == null ? BLACK : p.color;
    }

    private static <K, V> Node<K, V> parentOf(Node<K, V> p) {
        return p == null ? null : p.parent;
    }

    private static <K, V> void setColor(Node<K, V> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    private static <K, V> Node<K, V> leftOf(Node<K, V> p) {
        return p == null ? null : p.left;
    }

    private static <K, V> Node<K, V> rightOf(Node<K, V> p) {
        return p == null ? null : p.right;
    }

    // Проверка красно-чёрных инвариантов для тестов
    public boolean checkInvariants() {
        if (root == null) {
            return true;
        }
        if (root.color != BLACK) {
            return false;
        }
        return checkNodeInvariants(root) >= 0;
    }

    private int checkNodeInvariants(Node<K, V> node) {
        if (node == null) {
            return 1; // NIL-лист черный
        }

        // Красный узел не может иметь красных детей
        if (node.color == RED) {
            if (colorOf(node.left) == RED || colorOf(node.right) == RED) {
                return -1;
            }
        }

        int leftBlackHeight = checkNodeInvariants(node.left);
        int rightBlackHeight = checkNodeInvariants(node.right);

        if (leftBlackHeight == -1 || rightBlackHeight == -1 || leftBlackHeight != rightBlackHeight) {
            return -1;
        }

        return leftBlackHeight + (node.color == BLACK ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        inOrderString(root, sb, new boolean[]{true});
        return sb.append("}").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomTreeMap<?, ?> that = (CustomTreeMap<?, ?>) o;
        if (this.size != that.size) return false;
        return checkNodeEquals(this.root, that);
    }

    private boolean checkNodeEquals(Node<K, V> node, CustomTreeMap<?, ?> that) {
        if (node == null) return true;
        Object thatVal = that.get(node.key);
        if (thatVal == null) {
            if (node.value != null || !that.containsKey(node.key)) {
                return false;
            }
        } else if (!thatVal.equals(node.value)) {
            return false;
        }
        return checkNodeEquals(node.left, that) && checkNodeEquals(node.right, that);
    }

    @Override
    public int hashCode() {
        return calcNodeHashCode(root);
    }

    private int calcNodeHashCode(Node<K, V> node) {
        if (node == null) return 0;
        int h = Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
        return h + calcNodeHashCode(node.left) + calcNodeHashCode(node.right);
    }

    private void inOrderString(Node<K, V> node, StringBuilder sb, boolean[] first) {
        if (node != null) {
            inOrderString(node.left, sb, first);
            if (!first[0]) {
                sb.append(", ");
            }
            sb.append(node.key).append("=").append(node.value);
            first[0] = false;
            inOrderString(node.right, sb, first);
        }
    }
}
