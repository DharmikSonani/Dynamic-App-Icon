import { NativeModules, Platform } from 'react-native';
const { AppIconModule } = NativeModules;

export const getAppIcon = async () => { return await AppIconModule?.getAppIcon() };

const handleIconChange = async (icon) => {
    try {
        const currentIcon = await getAppIcon();
        if (currentIcon?.toLowerCase() !== icon?.toLowerCase() || Platform.OS === 'android') {
            await AppIconModule?.changeAppIcon(icon);
        }
    } catch (error) {
        console.log(error);
    }
}

export const useDefaultIcon = () => handleIconChange('default');
export const useIconB = () => handleIconChange('IconB');
export const useIconC = () => handleIconChange('IconC');