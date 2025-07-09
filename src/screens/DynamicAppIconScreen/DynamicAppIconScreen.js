import { FlatList, Platform, StyleSheet, Text } from 'react-native'
import React from 'react'
import IconCard from './components/IconCard'
import { useDefaultIcon, useIconB, useIconC } from '../../hooks/useAppIcons'

const data = [
    {
        display: require('./assets/default.png'),
        icon: useDefaultIcon,
    },
    {
        display: require('./assets/b.png'),
        icon: useIconB,
    },
    {
        display: require('./assets/c.png'),
        icon: useIconC,
    },
]

const DynamicAppIconScreen = () => {
    return (
        <FlatList
            data={data}
            showsVerticalScrollIndicator={false}
            renderItem={({ item }) => <IconCard data={item} />}
            keyExtractor={(item, index) => index.toString()}
            style={styles.Container}
            numColumns={2}
            contentContainerStyle={styles.ContentContainer}
            ListHeaderComponent={
                <Text style={styles.Header}>
                    Dynamic App Icons
                </Text>
            }
        />
    )
}

export default DynamicAppIconScreen

const styles = StyleSheet.create({
    Container: {
        flex: 1,
        backgroundColor: 'rgba(255,255,255,1)',
    },
    ContentContainer: {
        paddingTop: Platform.OS == 'ios' ? 70 : 40,
        width: '100%',
        paddingBottom: 20,
        paddingHorizontal: 20,
    },
    Header: {
        fontSize: 30,
        fontWeight: 'bold',
        alignSelf: 'center',
        color: 'rgba(0,0,0,1)',
        marginBottom: 10,
    },
});