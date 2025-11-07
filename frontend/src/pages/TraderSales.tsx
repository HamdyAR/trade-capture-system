import React from 'react';
import {useSearchParams} from "react-router-dom";
import Layout from "../components/Layout";
import {HomeContent} from "../components/HomeContent";
import {TradeBlotterModal} from "../modal/TradeBlotterModal";
import {TradeActionsModal} from "../modal/TradeActionsModal";
import {TradeActionsModalPrac} from "../modal/TradeActionsModalPrac";

const TraderSales = () => {
    const [searchParams] = useSearchParams();
    const view = searchParams.get('view') || 'default';

    return (
        <div>
            <Layout>
                {view === 'default' && <HomeContent/>}
                {view === 'actions' && <TradeActionsModalPrac/>}
                {/* {view === 'prac' && <TradeActionsModalPrac/>} */}
                {view === 'history' && <TradeBlotterModal/>}
            </Layout>
        </div>
    );
};

export default TraderSales;
