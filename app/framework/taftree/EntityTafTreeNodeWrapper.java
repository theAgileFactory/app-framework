package framework.taftree;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import framework.services.configuration.II18nMessagesPlugin;

/**
 * A wrapper of bean Entity to {@link ITafTreeNode}
 * 
 * @author Pierre-Yves Cloux
 * @param <E>
 *            the entity which is handled by the taf tree node
 */
public class EntityTafTreeNodeWrapper<E extends INodeEntity<E>> implements ITafTreeNode {
    private E entity;

    public EntityTafTreeNodeWrapper(E entity) {
        this.entity = entity;
    }

    /**
     * Convert a list of {@link INodeEntity} into a list of
     * {@link EntityTafTreeNodeWrapper}
     * 
     * @param reportingCategoryList
     * @return
     */
    public static <E extends INodeEntity<E>> List<EntityTafTreeNodeWrapper<E>> fromEntityList(List<E> entityList) {
        return entityList.stream().map(new Function<E, EntityTafTreeNodeWrapper<E>>() {
            @Override
            public EntityTafTreeNodeWrapper<E> apply(E entity) {
                return new EntityTafTreeNodeWrapper<E>(entity);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public Long getId() {
        return getEntity().getNodeId();
    }

    @Override
    public void setDeleted(boolean deleted) {
        getEntity().setNodeDeleted(deleted);
    }

    @Override
    public String getName() {
        return getEntity().getNodeName();
    }

    @Override
    public void setName(String name) {
        getEntity().setNodeName(name);
    }

    @Override
    public boolean isManageable() {
        return getEntity().isNodeManageable();
    }

    @Override
    public void setManageable(boolean manageable) {
        getEntity().setNodeManageable(manageable);
    }

    @Override
    public int getOrder() {
        return getEntity().getNodeOrder();
    }

    @Override
    public void setOrder(int order) {
        getEntity().setNodeOrder(order);
        ;
    }

    @Override
    public void setParent(Long parentId) {
        getEntity().setNodeParent(parentId);
        ;
    }

    @Override
    public EntityTafTreeNodeWrapper<E> getParent() {
        E parentEntity = getEntity().getNodeParent();
        if (parentEntity == null) {
            return null;
        }
        return new EntityTafTreeNodeWrapper<E>(parentEntity);
    }

    @Override
    public boolean hasChildren() {
        return getEntity().hasNodeChildren();
    }

    @Override
    public List<? extends ITafTreeNode> getChildren() {
        return fromEntityList(getEntity().getNodeChildren());
    }

    @Override
    public int getLastChildrenOrder() {
        return getEntity().getLastNodeChildrenOrder();
    }

    /**
     * Get the name in the correct language and if empty then get it in the
     * first available language.
     */
    public String getTranslatedName(II18nMessagesPlugin messagesPlugin) {
        return TafTreeHelper.getName(this, messagesPlugin);
    }

    /**
     * Same as getTranslatedName with full name.<br/>
     * parentName > Name
     */
    public String getTranslatedFullName(II18nMessagesPlugin messagesPlugin) {
        String r = "";
        EntityTafTreeNodeWrapper<E> tafTreeNodeParent = getParent();
        if (tafTreeNodeParent != null) {
            r = tafTreeNodeParent.getTranslatedName(messagesPlugin) + " > ";
        }
        return r + TafTreeHelper.getName(this, messagesPlugin);
    }

    /**
     * Return the root element of the tree
     */
    public EntityTafTreeNodeWrapper<E> getRoot() {
        E rootEntity = getEntity().getRootNode();
        if (rootEntity == null) {
            return null;
        }
        return new EntityTafTreeNodeWrapper<E>(rootEntity);
    }

    /**
     * Same as getTranslatedName with root name.<br/>
     * rootName > Name
     */
    public String getTranslatedRootName(II18nMessagesPlugin messagesPlugin) {
        String r = "";
        EntityTafTreeNodeWrapper<E> rootEntity = getRoot();
        if (rootEntity != null) {
            r = rootEntity.getTranslatedName(messagesPlugin) + " > ";
        }
        return r + TafTreeHelper.getName(this, messagesPlugin);
    }

    public E getEntity() {
        return entity;
    }
}
