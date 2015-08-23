package framework.taftree;

import java.util.List;

/**
 * Interface to be implemented by the entities which can be used as Tree nodes
 * @author Pierre-Yves Cloux
 * @param <E>
 */
public interface INodeEntity<E> {
    public Long getNodeId();
    public boolean isNodeDeleted();
    public void setNodeDeleted(boolean deleted);
    public String getNodeName();
    public void setNodeName(String name);
    public boolean isNodeManageable();
    public void setNodeManageable(boolean manageable);
    public E getNodeParent();
    public void setNodeParent(Long parentId);
    public boolean hasNodeChildren();
    public List<E> getNodeChildren();
    public int getLastNodeChildrenOrder();
    public int getNodeOrder();
    public void setNodeOrder(int order);
    public E getRootNode();
}
