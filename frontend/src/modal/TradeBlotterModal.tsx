import React from "react";
import {observer} from "mobx-react-lite";
import AGGridTable from "../components/AGGridTable";
import {fetchTrades} from "../utils/api";
import {getColDefFromResult, getRowDataFromData} from "../utils/agGridUtils";
import { useQuery } from '@tanstack/react-query';
import {Trade} from "../utils/tradeTypes";


export const TradeBlotterModal: React.FC = observer(() => {
    const [trades, setTrades] = React.useState<Trade[]>([]);

    const {data, isSuccess} = useQuery({
        queryKey: ['trades'],
        queryFn: async () => {
            const res = await fetchTrades();
            return res.data;
        },
        refetchInterval: 30000,
        refetchIntervalInBackground: true,
    });

    React.useEffect(() => {
        if (isSuccess && data) {
            const mappedData = (data as Trade[]).map((item: Trade) => {
                const settlementField = Array.isArray(item.additionalFields)
            ? item.additionalFields.find((f: any) =>f && typeof f === "object" && f.fieldName === "SETTLEMENT_INSTRUCTIONS")
            : undefined;

            const settlementInstructions = settlementField && settlementField.fieldValue ? settlementField.fieldValue : '';

            return{
                ...item,
                settlementInstructions
            }
            })



            setTrades(mappedData);
        }
    }, [isSuccess, data]);
    console.log(data);

    const columnDefs = getColDefFromResult(trades);
    const rowData = getRowDataFromData(trades);
    return (
        <div className={"h-fit w-full flex flex-col min-h-full min-w-full justify-start"}>
            <div>
                <AGGridTable columnDefs={columnDefs}
                             rowData={rowData}
                             onSelectionChanged={() => {
                             }}
                             rowSelection={"single"}/>
            </div>
        </div>
    )
})