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

/*
- Use "useCurrentIcon" hook only on Android in specific cases, such as Google authentication,
- where the Android activity might be stopped or backgrounded unexpectedly.
*/
export const useCurrentIcon = async () => Platform.OS === 'android' ? handleIconChange(await getAppIcon()) : {};