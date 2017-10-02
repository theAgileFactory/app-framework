/*****************************************************************
 * The taf tree jQuery plugin allows to display a node           *
 * tree with the availability to manage its entries.             *
 *                                                               *
 * Requirements:                                                 *
 * -jQuery 1.11.1                                                *
 * -Bootstrap 3.1.1                                              *
 * -Bootstrap X-editable 1.5.1                                   *
 *                                                               *
 *****************************************************************
 *                                                               *
 * Documentation                                                 *
 *                                                               *
 * For using this plugin, just follow those steps:               *
 *                                                               *
 * 1) Create a new object (usually stored in the DB) with at     *
 *    least the following attributes:                            *
 *      -an id                                                   *
 *      -a deleted flag                                          *
 *      -a name                                                  *
 *      -a manageable flag                                       *
 *      -an order flag                                           *
 *      -a parent id                                             *
 *                                                               *
 * 2) The object must implement the interface ITafTreeNode (see  *
 *    its content for more details)                              *
 *    This object is called NodeImpl in the next steps           *
 *                                                               *
 * 3) Create a new controller (with needed routes) to catch the  *
 *    3 mains actions of the plugin:                             *
 *                                                               *
 *      manage: allows to manage a node (add, edit...)           *
 *          Long id = TafTreeHelper.getId(request());            *
 *          NodeImpl node = null;                                *
 *          if (id == null) {                                    *
 *              node = new NodeImpl();                           *
 *          } else {                                             *
 *              node = NodeImpl.getById(id);                     *
 *          }                                                    *
 *          TafTreeHelper.fill(request(), node);                 *
 *          // do here what you want, for example saving the     *
 *          // node in a DB                                      *
 *          return ok(TafTreeHelper.get(node));                  *
 *                                                               *
 *      loadChildren: load the children of a node                *
 *          Long id = TafTreeHelper.getId(request());            *
 *          List<NodeImpl> nodes = null;                         *
 *          if (id == null) {                                    *
 *              nodes = NodeImpl.getRoots();                     *
 *          } else {                                             *
 *              nodes = NodeImpl.getChildren(id);                *
 *          }                                                    *
 *          return ok(TafTreeHelper.gets(nodes));                *
 *                                                               *
 *      click: display the content related to a node when        *
 *      clicking on its name (simply by getting the node id)     *
 *                                                               *
 * 4) Instanciate the plugin in a view, for example:             *
 *    <div class="row">                                          *
 *        <div class="col-md-5">                                 *
 *            <div id="node-tree"></div>                         *
 *        </div>                                                 *
 *        <div class="col-md-7">                                 *
 *            <h3>Node content</h3>                              *
 *            <div id="list"></div>                              *
 *        </div>                                                 *
 *    </div>                                                     *
 *    <script type="text/javascript">                            *
 *      $.tafTree('#node-tree', {                                *
 *          'listViewable' : false,                              *
 *          'manageAction' : '/node/manage',                     *
 *          'loadChildrenAction' : '/node/children',             *
 *          'clickAction' : '/node/click',                       *
 *          'clickContainerId' : 'list'                          *
 *      });                                                      *
 *    </script>                                                  *
 *                                                               *
 *****************************************************************
**/

(function($){

    $.tafTree = function(selector, options) {

        /*
         * Translations.
         */
        var __transl = new Array();
        __transl['en'] = {
            'new' : 'New',
            'empty' : 'The tree is currently empty.',
            'error' : 'An error occurred, please try again.',
            'name_label' : 'Specify a name',
            'name_error' : 'This field is required',
            'delete_error' : 'Impossible to delete an entry which contains sub-elements.',
            'delete_confirm' : 'Are you sure you want to delete this entry?',
            'up_error' : 'This node is already on the top',
            'down_error': 'This node is already on the bottom',
            'node_deleted' : 'This entry no longer exists.'
        };
        __transl['fr-CH'] = {
            'new' : 'Nouveau',
            'empty' : 'L\'arbre est vide.',
            'error' : 'Une erreur est survenue, veuiller ressayer.',
            'name_label' : 'Saisir un nom',
            'name_error' : 'Ce champ est requis',
            'delete_error' : 'Impossible de supprimer une entrée qui contient des sous-éléments.',
            'delete_confirm' : 'Etes-vous sûr(e) de vouloir supprimer cette entrée?',
            'up_error' : 'Impossible de monter cette entrée.',
            'down_error': 'Impossible de déscendre cette entrée.',
            'node_deleted' : 'Cette entrée n\'existe plus.'
        };
        __transl['de'] = {
            'new' : 'Neu',
            'empty' : 'Der Baum ist derzeit leer.',
            'error' : 'Ein Fehler ist aufgetreten, bitte wiederholen.',
            'name_label' : 'Geben Sie einen Namen',
            'name_error' : 'Pflichtfeld',
            'delete_error' : 'Löschen nicht möglich da der Eintrag enthält Unterelemente.',
            'delete_confirm' : 'Sind Sie sicher, dass Sie diesen Eintrag löschen wollen?',
            'up_error' : 'Dieser Knoten ist bereits auf der Oberseite.',
            'down_error': 'Dieser Knoten ist bereits auf der Unterseite.',
            'node_deleted' : 'Dieser Eintrag existiert nicht mehr.'
        };

        /*
         * Default configuration.
         */
        var config = {
            'defaultName': null, //define the default name when creating a new node
            'manageable': true, //set to true if the nodes could by managed (add/remove/edit)
            'rootAddable': true, //set to true if it's possible to add root node
            'listViewable': true, //set to true if a the link to a flat list of a all nodes must be displayed 
            'title': '&nbsp;', //define the title of the tree
            'currentId': null, //define the id of the node currently selected (to highlight it)
            'nbDisplayedLevels': 2, //define the number of levels to display at initial loading
            'manageAction' : null, //define the controller action to manage the nodes (POST)
            'loadChildrenAction': null, //define the controller action to load the children nodes of a node (POST)
            'clickAction' : null, //define the controller action when clicking on a node (GET)
            'triggerClickActionAtStart' : true, //if the click action is defined and this flag is true, then the action is called at start
            'triggerClickActionAfterManage' : false, //if true then the click action is reloaded after a management action
            'listAction' : null, //define the controller action when clicking the list button (direct link without ajax) (GET)
            'clickIdName' : 'id', //define the parameter name used for the id when the click action is called
            'clickContainerId' : null, //define the id of the container (in the DOM) where the result of the click action is settled
            'language': 'en', //define the language of the messages
            'manualManageAction' : null //define the controller action to "manually" add/edit a node (when null the standard manageAction system is used) (GET)
                                //for add: the system provides the parent id node with a parameter called "parentId" (except for a root node) 
                                //and the order with a parameter called "order"
                                //for edit: the system provides the node id with a parameter called "id"
                                //in both cases, the call is done by "ajax", so the action should return an HTML fragment
        };
        
        /*
         * Merge the default configuration with the given options.
         */
        if (options) {
            $.extend(config, options);
        }
        
        /*
         * Get the translations in the correct language.
         */
        var transl = __transl[config.language];
        if (config.defaultName == null) {
            config.defaultName = transl.new;
        }
        
        /*
         * Define the main container.
         */
        var container = $(selector);
        
        /*
         * The "data" is THE root node, meaning its children are the true root node
         * data is simply a "fake" (not displayed) node used to have the common node properties
         */
        var data = new Node(0, "", false, 0, false, 0);

        /*
         * Construct the header.
         */
        
        // create the DOM elements
        var headerContainerDom = $('<div/>', {'class' : 'panel-heading'});
        var headerTitleDom = $('<h4/>');
        var headerActionsContainerDom = $('<span/>', {'class' : 'pull-right'});
        var headerAddActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-plus"></span>', 'id' : '_taf-tree-header-add-root'});
        var headerListActionDom = $('<a/>', {'href' : config.listAction, 'html' : '<span class="glyphicon glyphicon-list"></span>'});
        
        // display the "list" icon (with link) if needed
        if (config.listViewable && config.listAction != null) {
            headerActionsContainerDom.append(headerListActionDom);
        }

        // display the "add root node" icon if needed
        if (config.manageable && config.rootAddable) {

            // when clicking on add, we directly create the root node with the default name
            headerAddActionDom.click(function(event) {
                event.preventDefault();
                    
                if (config.manualManageAction == null) {
                    manage({ 'action' : 'addRoot', 'name' : config.defaultName, 'order' : data.lastChildrenOrder + 1 }, function(result) {
                        var _newNode = new Node(result.id, result.name, result.manageable, result.order, false, 0);
                        addNode(true, _newNode, data, false, false);
                    });
                } else {
                    $.ajax({
                        type: 'GET',
                        url: config.manualManageAction,
                        data: {"order" : data.lastChildrenOrder + 1},
                        cache : false,
                        error: function() {
                            alert(transl.error);
                        }
                    }).done(function(result) {
                        $("#" + config.clickContainerId).html(result);
                    });
                }
            });

            headerActionsContainerDom.append("&nbsp;&nbsp;");
            headerActionsContainerDom.append(headerAddActionDom);
        }
        
        headerTitleDom.append(config.title);
        headerTitleDom.append(headerActionsContainerDom);
        
        headerContainerDom.append(headerTitleDom);
        
        /*
         * Construct the tree.
         */

        var contentContainerDom = $('<div/>', {'class' : 'panel-body taf-tree', 'id' : '_taf-tree-view'});
        loadInitialNodes(1, null, data);
        
        // trigger the "clickAction" if required
        if (config.triggerClickActionAtStart) {
            callClickAction(config.currentId);
        }
        
        /*
         * Add the elements to the main container.
         */
        
        var mainContainerDom = $('<div/>', {'class' : 'panel panel-default'});
        mainContainerDom.append(headerContainerDom);
        mainContainerDom.append(contentContainerDom);
        container.append(mainContainerDom);

        return {

            /**
             * Helper to submit a form when manualManageAction is used.
             * 
             * @param _formId the form id
             */
            submitManageForm: function(_formId) {
                $( "#" + _formId ).submit(function( event ) {
                    event.preventDefault();
                    var $form = $( this ),
                    url = $form.attr( "action" );
                    $.post(url, $form.serialize()).done(function( result ) {
                        $("#" + config.clickContainerId).html(result);
                    });
                });
            },
            
            /**
             * Helper to add a node when manualManageAction is used.
             */
            addNode: function(_parentId, _id, _name, _manageable, _order) {
                var _newNode = new Node(_id, _name, _manageable, _order, false, 0);
                if(_parentId == null) {
                    addNode(true, _newNode, data, false, true);
                } else {
                    addNode(true, _newNode, findNode(_parentId, data), false, true);
                }
                
            },
            
            /**
             * Helper to edit a node when manualManageAction is used.
             */
            editNode: function(_id, _name) {
                $("#_taf-tree-name-" + _id).html(_name);
            },
            
            /**
             * Helper to set the currentId and load the clickAction when manualManageAction is used.
             */
            setCurrentIdAndLoadClickAction: function(_id) {
                config.currentId = _id;
                
                $('#_taf-tree-view li span i').css("font-weight","normal");
                $("#_taf-tree-name-" + _id).css("font-weight","bold");
                
                callClickAction(_id);
            },
            
            triggerEditNode: function(_id) {
                $.ajax({
                    type: 'GET',
                    url: config.manualManageAction,
                    data: {"id" : _id},
                    cache : false,
                    error: function() {
                        alert(transl.error);
                    }
                }).done(function(result) {
                    $("#" + config.clickContainerId).html(result);
                });
            }
        };
        

        /**
         * Find a node from the loaded ones.
         * 
         * @param _id the node id
         * @param _node the first node to throw
         */
        function findNode(_id, _node) {
            if (_node.id == _id) {
                return _node;
            }
            
            if (_node.hasChildren) {
                for (var i = 0; i < _node.children.length; ++i) {
                    var _subNode = findNode(_id, _node.children[i]);
                    if (_subNode != null) {
                        return _subNode;
                    }
                }
            }
            
            return null;
        }
        
        
        /**
         * Load the initial nodes until the number of levels to display is reach.
         * 
         * Note: this method is recursive.
         * 
         * @param _loop the current iteration (1 at start)
         * @param _parentId the id of the parent node (null at start)
         * @param _parentNode the parent node ("THE root node" at start)
         */
        function loadInitialNodes(_loop, _parentId, _parentNode) {

            $.ajax({
                type: 'POST',
                contentType: "application/json; charset=utf-8",
                dataType: 'json',
                url: config.loadChildrenAction,
                data: JSON.stringify({'id' : _parentId}),
                cache : false,
                error: function() {
                    alert(transl.error);
                }
            }).done(function(result) {

                // if the first iteration returns no result (meaning there is no node)
                // then display the empty message
                if (_loop == 1 && result.length == 0) {
                    contentContainerDom.append(transl.empty);
                }
                
                // if the first iteration return results
                // then adapt the layout of the content container and update THE root node
                if (_loop == 1 && result.length != 0) {
                    contentContainerDom.addClass('parent_li');
                    contentContainerDom.append($('<ul/>'));
                    data.hasChildren = true;
                    data.lastChildrenOrder = result[result.length - 1].order;
                }
                
                var _hasChildren = false;

                // for each child of the node
                for (i = 0; i < result.length; ++i) {

                    // create a node instance for the child node
                    var _node = new Node(result[i].id, result[i].name, result[i].manageable, result[i].order, result[i].hasChildren, result[i].lastChildrenOrder);
                    
                    // set to true the isCollapsed attribute if the current iteration is the last and if the child node has children
                    var _isCollapsed = false;
                    if(result[i].hasChildren && _loop == config.nbDisplayedLevels) {
                        _isCollapsed = true;
                    }
                    
                    // add and draw the child node
                    addNode(false, _node, _parentNode, _isCollapsed, false);

                    // if the current iteration is not the last and if the child node has children then call recursively the loadInitialNodes method
                    if (result[i].hasChildren && _loop < config.nbDisplayedLevels) {
                        _hasChildren = true;
                        loadInitialNodes(_loop + 1, result[i].id, _node);
                    }

                }
                
                // if the current iteration is the last then apply the bold style to the current selected node
                if ((_loop == config.nbDisplayedLevels || _hasChildren == false) && config.currentId != null) {
                    $("#_taf-tree-name-" + config.currentId).css("font-weight","bold");
                }
            });
        }
        
        /**
         * Add a node to the data object and draw it.
         * 
         * @param _isNew set to true if the node has just been created (meaning not called by loadInitialNodes)
         * @param _node the node to add
         * @param _parentNode the direct parent of the node to add
         * @param _isCollapsed set to true if the node must by default collapsed (if it has children)
         * @param _isManually set to true if the node creation (in the DB) has been done manually
         */
        function addNode(_isNew, _node, _parentNode, _isCollapsed, _isManually) {

            // if the whole tree hasn't node, then we remove the "empty" message
            if (!data.hasChildren) {
                contentContainerDom.html('');
            }

            // get the parent container:
            // -root node: it's represented by the tree content container
            // -other: it's represented by the "li" tag of the parent node
            if (_parentNode.id == 0) {
                var _parentContainerDom = contentContainerDom;
            } else {
                var _parentContainerDom = $('#_taf-tree-li-' + _parentNode.id);
            }
            
            // in new case, if the parent node hasn't children (meaning the new node to add is the first children)
            // then add the "parent_li" class and a "ul" tag
            if (_isNew && !_parentNode.hasChildren) {

                _parentContainerDom.addClass('parent_li');
                _parentContainerDom.append($('<ul/>'));
                
                // if the parent is not the main container, we also need to add the collapse button
                if (_parentNode.id != 0) {
                
                    var _collapseButtonDom = $('<i/>', {'html' : '<i class="glyphicon glyphicon-minus-sign"></i>&nbsp;&nbsp;'});
                    addCollapseAction(_collapseButtonDom);
                    
                    _parentContainerDom.children("span").prepend(_collapseButtonDom);
                }
            
            }
            
            // add the node in the current container
            _parentContainerDom.children("ul").append(_node.draw(_isCollapsed));
            
            // if the node can be managed
            if (config.manageable) {
            
                // add the manage actions
                drawActions(_node, _parentNode);
                
                // if new and not manually case then show the edit form
                if (_isNew && !_isManually) {
                    $("#_taf-tree-name-" + _node.id).editable('toggle');
                }

            }
            
            // add the node as a children of the parent
            _parentNode.addChildren(_node);
        }
        
        /**
         * Remove a node from the data object and the display
         * 
         * @param _node the node to remove
         * @param _parentNode the direct parent of the node to remove
         */
        function removeNode(_node, _parentNode) {

            // get the parent container:
            // -root node: it's represented by the tree content container
            // -other: it's represented by the "li" tag of the parent node
            if (_parentNode.id == 0) {
                var _parentContainerDom = contentContainerDom;
            } else {
                var _parentContainerDom = $('#_taf-tree-li-' + _parentNode.id);
            }

            // remove from display
            $('#_taf-tree-li-' + _node.id).remove();
            
            //remove from data object
            _parentNode.removeChildren(_node);
            
            // if the node to remove was the last children of the parent node
            // then remove the "parent" display
            if (!_parentNode.hasChildren) {
                _parentContainerDom.removeClass('parent_li');
                _parentContainerDom.children("ul").remove();
                _parentContainerDom.children("span").children("i").first().remove();
            }
           
            // if the whole tree hasn't node, then set the empty message
            if (!data.hasChildren) {
                contentContainerDom.html(transl.empty);
            }
            
        }
        
        /**
         * draw the manage actions of a node
         * this method is called when a node is manageable
         * 
         * @param _node the node for which the actions should be added
         * @param _node the parent node of the node (useful for changing order)
         */
        function drawActions(_node, _parentNode) {

            // create the DOM elements
            var _leafAddActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-plus"></span>'});
            var _leafUpActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-chevron-up"></span>'});
            var _leafDownActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-chevron-down"></span>'});
            
            if (_node.manageable) {
                var _leafEditActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-pencil"></span>'});
                var _leafDeleteActionDom = $('<a/>', {'href' : '#', 'html' : '<span class="glyphicon glyphicon-trash"></span>'});
            }
            
            // add event for the ADD action
            _leafAddActionDom.click(function(event) {
                event.preventDefault();

                // force an elapse of the node (nothing is done if already elasped or hasn't child)
                $('#_taf-tree-leaf-'+ _node.id).find(".glyphicon-plus-sign").trigger('click');
                    
                if (config.manualManageAction == null) {

                    manage({ 'action' : 'add', 'name' : config.defaultName, 'order' : _node.lastChildrenOrder + 1, 'parentId' : _node.id }, function(result) {
                        var _newNode = new Node(result.id, result.name, result.manageable, result.order, false, 0);
                        addNode(true, _newNode, _node, false, false);
                    });
                    
                } else {
                    
                    $.ajax({
                        type: 'GET',
                        url: config.manualManageAction,
                        data: {"order" : _node.lastChildrenOrder + 1, "parentId" : _node.id},
                        cache : false,
                        error: function() {
                            alert(transl.error);
                        }
                    }).done(function(result) {
                        $("#" + config.clickContainerId).html(result);
                    });
                    
                }
                    
            });
            
            //add events for UP action
            _leafUpActionDom.click(function(event) {
                event.preventDefault();

                // get the key of the current node
                var _key = _parentNode.children.indexOf(_node);

                if (_key == 0) {
                    alert(transl.up_error);
                } else {
                    
                    // get the previous node
                    var _previousNode = _parentNode.children[_key - 1];
                    
                    // store the orders
                    var _nodeOrder = _node.order;
                    var _previousNodeOrder = _previousNode.order;
                    
                    // first we set the order of the node to the order of the previous one
                    manage({ 'action' : 'changeOrder', 'id' : _node.id, 'order' : _previousNodeOrder }, function(result) {
                    
                        // then we set the order of the previous node to the order of the current one
                        manage({ 'action' : 'changeOrder', 'id' : _previousNode.id, 'order' : _nodeOrder }, function(result) {
                            
                            // first we update the order of nodes
                            _node.order = _previousNodeOrder;
                            _previousNode.order = _nodeOrder;
                            
                            // then we invert the 2 nodes in the children table
                            _parentNode.children[_key - 1] = _node;
                            _parentNode.children[_key] = _previousNode;
                            
                            // finally we update the display
                            var _liNodeDom = $('#_taf-tree-li-' + _node.id);
                            var _liPreviousNodeDom = $('#_taf-tree-li-' + _previousNode.id);
                            _liPreviousNodeDom.before(_liNodeDom);
                            
                        });
                    });
                }
            });
            
            //add events for DOWN action
            _leafDownActionDom.click(function(event) {
                event.preventDefault();

                // get the key of the current node
                var _key = _parentNode.children.indexOf(_node);

                if (_key == _parentNode.children.length - 1) {
                    alert(transl.down_error);
                } else {
                    
                    // get the next node
                    var _nextNode = _parentNode.children[_key + 1];
                    
                    // store the orders
                    var _nodeOrder = _node.order;
                    var _nextNodeOrder = _nextNode.order;
                    
                    // first we set the order of the node to the order of the next one
                    manage({ 'action' : 'changeOrder', 'id' : _node.id, 'order' : _nextNodeOrder }, function(result) {

                        // then we set the order of the next node to the order of the current one
                        manage({ 'action' : 'changeOrder', 'id' : _nextNode.id, 'order' : _nodeOrder }, function(result) {
                            
                            // first we update the order of nodes
                            _node.order = _nextNodeOrder;
                            _nextNode.order = _nodeOrder;
                            
                            // then we invert the 2 nodes in the children table
                            _parentNode.children[_key + 1] = _node;
                            _parentNode.children[_key] = _nextNode;
                            
                            // finally we update the display
                            var _liNodeDom = $('#_taf-tree-li-' + _node.id);
                            var _liNextNodeDom = $('#_taf-tree-li-' + _nextNode.id);
                            _liNextNodeDom.after(_liNodeDom);
                            
                        });
                    });
                }
            });
            
            if (_node.manageable) {
                
                //add events for the EDIT action
                if (config.manualManageAction == null) {

                    $("#_taf-tree-name-" + _node.id).editable({
                        title : transl.name_label,
                        emptytext : '',
                        type : 'text',
                        value: _node.name,
                        toggle: 'manual',
                        validate: function(value) {
                            if($.trim(value) == '') {
                                return transl.name_error;
                            }
                        },
                        success: function(response, newName) {
                            manage({ 'action' : 'edit', 'name' : newName, 'id' : _node.id });
                        }
                    });
                    
                    _leafEditActionDom.click(function(e){
                        e.preventDefault();
                        e.stopPropagation();
                        $("#_taf-tree-name-" + _node.id).editable('toggle');
                    });
                
                } else {
                    
                    _leafEditActionDom.click(function(e){
                        e.preventDefault();
                    
                        $.ajax({
                            type: 'GET',
                            url: config.manualManageAction,
                            data: {"id" : _node.id},
                            cache : false,
                            error: function() {
                                alert(transl.error);
                            }
                        }).done(function(result) {
                            $("#" + config.clickContainerId).html(result);
                        });
                    
                    });
                    
                }
                
                //add events for the DELETE action
                _leafDeleteActionDom.click(function(event) {
                    event.preventDefault();
                    
                    // if the node to remove has children then display an error
                    if (_node.hasChildren) {
                        alert(transl.delete_error);
                    } else {
                        if (confirm(transl.delete_confirm)) {
                            manage({ 'action' : 'delete', 'id' : _node.id }, function(result) {
                                removeNode(_node, _parentNode);
                                if (config.currentId == _node.id) {
                                    $("#" + config.clickContainerId).html(transl.node_deleted);
                                }
                            });
                        }
                    }
                    
                });
            
            }

            // draw the actions
            var _leafActionsContainer = $('<span/>', {'id' : '_taf-tree-leaf-actions'});
            _leafActionsContainer.append("&nbsp;&nbsp;");
            _leafActionsContainer.append(_leafAddActionDom);
            if (_node.manageable) {
                _leafActionsContainer.append("&nbsp;");
                _leafActionsContainer.append(_leafEditActionDom);
                _leafActionsContainer.append("&nbsp;");
                _leafActionsContainer.append(_leafDeleteActionDom);
            }
            _leafActionsContainer.append("&nbsp;");
            _leafActionsContainer.append(_leafUpActionDom);
            _leafActionsContainer.append("&nbsp;");
            _leafActionsContainer.append(_leafDownActionDom);
            
            $('#_taf-tree-leaf-'+ _node.id).append(_leafActionsContainer);

        }

        /**
         * Define a node.
         * 
         * @param _id the node id
         * @param _name the node name
         * @param _manageable set to true if the node can be managed
         * @param _order the node order
         * @param _hasChildren set to true if the node has children
         * @param _lastChildrenOrder higher children order (0 if not child)
         */
        function Node(_id, _name, _manageable, _order, _hasChildren, _lastChildrenOrder) {
        
            this.id = _id;
            this.name = _name;
            this.manageable = _manageable;
            this.order = _order;
            this.hasChildren = _hasChildren;
            this.lastChildrenOrder = _lastChildrenOrder;
            
            this.children = [];
            
            this.addChildren = function(_node) {
                this.hasChildren = true;
                this.lastChildrenOrder = _node.order;
                this.children.push(_node);
            };
            
            this.removeChildren = function(_node) {
                var _key = this.children.indexOf(_node);
                this.children.splice(_key, 1);
                if (this.children.length == 0) {
                    this.hasChildren = false;
                    this.lastChildrenOrder = 0;
                } else {
                    var _lastChildren = this.children[this.children.length - 1];
                    this.lastChildrenOrder = _lastChildren.order;
                }
            };

            this.draw = function(_isCollapsed) {
                
                var self = this;

                // create the main item (li)
                var _liDom = $('<li/>', {'id' : '_taf-tree-li-' + this.id});
                
                // during loading phase, if it has children add the "parent_li" class
                if (this.hasChildren) {
                    _liDom.addClass("parent_li");
                }
                
                // create the leaf
                var _spanDom = $('<span/>', {'class' : 'leaf', 'id' : '_taf-tree-leaf-' + this.id});
                
                // during loading phase, if it has children add the collapse button
                if (this.hasChildren) {

                    // if the node should be collapsed by default (meaning we don't know its children)
                    // then adapt the action when clicking on the collapse button
                    if (_isCollapsed) {
                        
                        var _collapseButtonDom = $('<i/>', {'html' : '<i class="glyphicon glyphicon-plus-sign"></i>&nbsp;&nbsp;'});
                        
                        _collapseButtonDom.click(function(event) {
                            event.stopPropagation();
                            
                            $.ajax({
                                type: 'POST',
                                contentType: "application/json; charset=utf-8",
                                dataType: 'json',
                                url: config.loadChildrenAction,
                                data: JSON.stringify({'id' : self.id}),
                                cache : false,
                                error: function() {
                                    alert(transl.error);
                                }
                            }).done(function(result) {
                                
                                _collapseButtonDom.unbind('click');
                                _collapseButtonDom.children('i').removeClass('glyphicon-plus-sign');
                                _collapseButtonDom.children('i').addClass('glyphicon-minus-sign');
                                addCollapseAction(_collapseButtonDom);
                                
                                for (i = 0; i < result.length; ++i) {
                                    var _node = new Node(result[i].id, result[i].name, result[i].manageable, result[i].order, result[i].hasChildren, result[i].lastChildrenOrder);
                                    addNode(false, _node, self, true, false);
                                }
                            });
                        });
                        

                    } else {
                        var _collapseButtonDom = $('<i/>', {'html' : '<i class="glyphicon glyphicon-minus-sign"></i>&nbsp;&nbsp;'});
                        addCollapseAction(_collapseButtonDom);
                    }
                    
                    _spanDom.append(_collapseButtonDom);

                }
                
                // add the node name to the leaf
                _nameDom = $('<i/>', {'id' : '_taf-tree-name-' + this.id, 'html' : this.name});
                _spanDom.append(_nameDom);
                
                // add the click action
                _nameDom.click(function(event) {
                    event.stopPropagation();
                    
                    config.currentId = self.id;
                    
                    $('#_taf-tree-view li span i').css("font-weight","normal");
                    $(this).css("font-weight","bold");
                    
                    callClickAction(self.id);
                });

                // add the leaf to the main item
                _liDom.append(_spanDom);

                // during loading phase, if it has children add a ul tag
                if (this.hasChildren) {
                    _liDom.append($('<ul/>'));
                }
                
                return _liDom;
            };
        }
        
        /**
         * Call the ajax manage action
         * 
         * @param _params the parameters to describe the type and the values for the manage action
         * @param _success the success function (called only if success)
         */
        function manage(_params, _success) {
            $.ajax({
                type: 'POST',
                contentType: "application/json; charset=utf-8",
                dataType: 'json',
                url: config.manageAction,
                data: JSON.stringify(_params),
                cache : false,
                error: function(jqXHR, textStatus, errorThrown) {
                    if (jqXHR.responseText != "") {
                        alert(jqXHR.responseText);
                    } else {
                        alert(transl.error);
                    }
                }
            }).done(function(result) {
                if (typeof _success !== 'undefined') {
                    _success(result);
                }
                if(config.triggerClickActionAfterManage) {
                    callClickAction(config.currentId);
                }
            });
        }
        
        /**
         * Add the collapse action to the collapse button of a leaf
         * 
         * @param _collapseButtonDom the collapse button
         * @returns
         */
        function addCollapseAction(_collapseButtonDom) {
            _collapseButtonDom.click(function(event) {
                event.stopPropagation();
                var _children = $(this).parent('span').parent('li.parent_li').find(' > ul > li');
                if (_children.is(":visible")) {
                    _children.hide('fast');
                    $(this).children("i").addClass('glyphicon-plus-sign').removeClass('glyphicon-minus-sign');
                } else {
                    _children.show('fast');
                    $(this).children("i").addClass('glyphicon-minus-sign').removeClass('glyphicon-plus-sign');
                }
                
            });
        }
        
        /**
         * Call the click action.
         * 
         * @param _id the node id
         */
        function callClickAction(_id) {
            
            var _params = {};
            if (_id != null) {
                _params[config.clickIdName] = _id;
            }

            $.ajax({
                type: 'GET',
                url: config.clickAction,
                data: _params,
                cache : false,
                error: function() {
                    alert(transl.error);
                }
            }).done(function(result) {
                $("#" + config.clickContainerId).html(result);
            });
        }

    };


})(jQuery);