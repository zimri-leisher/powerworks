package mod

import data.FileManager
import data.GameDirectoryIdentifier
import main.Game
import java.security.*

class ModPermissionsPolicy : Policy() {

    override fun getPermissions(codesource: CodeSource?): PermissionCollection {
        val p = Permissions()
        if(codesource != null && codesource.location != null) {
            if (codesource.location.path == Game.JAR_PATH) {
                p.add(AllPermission())
            }
        }
        p.add(AllPermission())
        return p
    }

    override fun refresh() {
    }
}