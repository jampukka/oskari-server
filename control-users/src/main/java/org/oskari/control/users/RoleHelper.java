package org.oskari.control.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.oskari.common.ServiceFactory;

import fi.mml.portti.domain.permissions.Permissions;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.data.domain.OskariLayerResource;
import fi.nls.oskari.permission.domain.Permission;
import fi.nls.oskari.permission.domain.Resource;
import fi.nls.oskari.user.IbatisRoleService;
import fi.nls.oskari.util.JSONHelper;

public class RoleHelper {
    
    private static final Logger LOG = LogFactory.getLogger(RoleHelper.class);
    
    public static void savePermissions(final OskariLayer layer, 
            final IbatisRoleService roleService, final JSONObject rolePermissions) {
        final List<Permission> permissions = RoleHelper.parsePermissions(
                roleService, rolePermissions);
        final Resource resource = RoleHelper.createPermissionResource(layer, permissions);
        if (resource != null) {
            ServiceFactory.getPermissionsService().saveResourcePermissions(resource);
        }
    }
    
    
    public static List<Permission> parsePermissions(IbatisRoleService roleService, JSONObject permissionsJSON) {
        if (roleService == null || permissionsJSON == null) {
            return Collections.emptyList();
        }
    
        List<Permission> permissions = new ArrayList<>();
        /*
        "role_permissions": {
            "Guest" : ["VIEW_LAYER"],
            "User" : ["VIEW_LAYER"],
            "Administrator" : ["VIEW_LAYER"]
        }
        */
        List<String> roleNames = JSONHelper.getKeys(permissionsJSON);
        for (String roleName : roleNames) {
            final String roleId = getRoleId(roleService, roleName);
            if (roleId == null) {
                continue;
            }
            final List<String> types = JSONHelper.getArrayAsList(
                    permissionsJSON.optJSONArray(roleName));
            for (String type : types) {
                permissions.add(getExternalRolePermission(roleId, type));
            }
        }
        return permissions;
    }

    protected static String getRoleId(final IbatisRoleService service, String roleName) {
        final Role role = service.findRoleByName(roleName);
        if (role == null) {
            LOG.warn("Couldn't find matching role in DB:", roleName, "- Skipping!");
            return null;
        }
        return Long.toString(role.getId());
    
    }

    protected static Permission getExternalRolePermission(String roleId, String type) {
        final Permission permission = new Permission();
        permission.setExternalId(roleId);
        permission.setExternalType(Permissions.EXTERNAL_TYPE_ROLE);
        permission.setType(type);
        return permission;
    }

    public static Resource createPermissionResource(OskariLayer layer, List<Permission> permissions) {
        if (layer == null || permissions == null || permissions.size() == 0) {
            return null;
        }

        final Resource res = new OskariLayerResource(layer);
        for (Permission permission : permissions) {
            res.addPermission(permission);
        }
        return res;
    }
}
