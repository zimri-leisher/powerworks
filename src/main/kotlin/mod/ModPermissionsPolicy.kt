package mod

import data.FileManager
import data.GameDirectoryIdentifier
import main.Game
import java.security.*

class ModPermissionsPolicy : Policy() {

    override fun getPermissions(codesource: CodeSource?): PermissionCollection {
        val p = Permissions()
        if(codesource != null) {
            if (codesource.location.path == FileManager.fileSystem.getPath(GameDirectoryIdentifier.JAR).toString()) {
                p.add(AllPermission())
            }
        }
        p.add(AllPermission())
        return p
    }

    override fun refresh() {
    }
}