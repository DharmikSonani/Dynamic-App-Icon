import { Image, StyleSheet, TouchableOpacity, } from 'react-native'
import React, { memo } from 'react'

const IconCard = ({
    data,
}) => {
    return (
        <TouchableOpacity
            style={styles.Button}
            onPress={() => { data?.icon() }}
        >
            <Image
                source={data?.display}
                style={styles.IconStyle}
                resizeMode='contain'
            />
        </TouchableOpacity>
    )
}

export default memo(IconCard)

const styles = StyleSheet.create({
    Button: {
        flex: 1,
        aspectRatio: 1 / 1,
        padding: 20,
        borderRadius: 30,
        margin: 10,
        backgroundColor: 'rgba(246,246,246,1)',
    },
    IconStyle: {
        height: '100%',
        width: '100%',
    },
})