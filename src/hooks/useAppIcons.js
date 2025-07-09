import { NativeModules } from 'react-native';
const { AppIconModule } = NativeModules;

export const getAppIcon = async () => { return await AppIconModule?.getAppIcon() };

const handleIconChange = async (icon) => {
    try {
        const currentIcon = await getAppIcon();
        if (currentIcon?.toLowerCase() !== icon?.toLowerCase()) AppIconModule?.changeAppIcon(icon)?.then(console.log)?.catch(console.log);
    } catch (error) {
        console.log(error);
    }
}

export const useDefaultIcon = () => handleIconChange('default');
export const useIconB = () => handleIconChange('IconB');
export const useIconC = () => handleIconChange('IconC');