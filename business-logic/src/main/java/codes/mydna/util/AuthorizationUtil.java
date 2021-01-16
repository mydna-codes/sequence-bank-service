package codes.mydna.util;

import codes.mydna.auth.common.RoleAccess;
import codes.mydna.auth.common.enums.Privilege;
import codes.mydna.auth.common.models.User;
import codes.mydna.exceptions.UnauthorizedException;
import codes.mydna.lib.enums.SequenceAccessType;

public class AuthorizationUtil {

    public static void verifyModification(SequenceAccessType accessType, User user){

        if(accessType == SequenceAccessType.PUBLIC) {
            if (!RoleAccess.canAccess(Privilege.MODIFY_PUBLIC_SEQUENCE, user))
                throw new UnauthorizedException("No sufficient rights to modify or create "
                        + SequenceAccessType.PUBLIC + " sequence.");
        }

    }

}
